# TCP Inboundで100スレッドまでがんばる会


## 検証方法

* application.ymlでスレッド数を操作してテストケースを実行すればなんとなくわかると思う。
* TCP Inbound=>ServiceActivatorから起動されるサービスで100スレッドが同時接続されるまでがんばらせるとか過酷な条件を強いられているんだ。
(CountDownLatch使えば楽)

## ハマったこと

### サーバー側はなぜか+1だけ余計にスレッドを食う

TCPConnectionFactoryで最大接続数 + 1のスレッドプールを用意する必要がある。Integrationが余計にSocket#accept()しているようだが、詳細は調べきれていない。
最大接続数と同数のサーバスレッドプールを用意すると、+1分のスレッド待ちキュー送りにされてつまる。
CFにtaskExecutorを指定しなくてもサーバ側で+1スレッド余計に起動している事象はデバッガなどで確認できる。

![最大接続数1でも余計なサーバースレッドが起動している](https://raw.githubusercontent.com/yggd/tcp-perfdemo/master/plusonethread.png)

### ServerSocketのバックログを明示指定する。

Javaのデフォルトだと50らしいが20あたりからConnection Resetが発生して明らかに足りていない。
https://docs.oracle.com/javase/jp/8/docs/api/java/net/ServerSocket.html#ServerSocket-int-

> 受信する接続(接続要求)のキューの最大長は、50に設定されます。キューが埋まっているときに接続要求があると、接続は拒否されます。
> 
> backlog引数は、ソケットの保留されている接続の要求された最大数です。正確なセマンティックスは実装に固有です。たとえば、実装が最大長を規定していたり、パラメータをまったく無視したりする場合があります。指定される値は0より大きくなければいけません。0以下の場合は、実装固有のデフォルトが使用されます。



integration.xml のbacklogを明示してみること。ただしAPI docにある通り、プラットフォームによって効果は異なる恐れがある。
Mac 10.14だと100指定でうまくいっている。 +
同じPOSIXのLinuxを信じたい。(Docker?なんですそれ?)

### (クライアント側) Socket#read()でレスポンスデータ終了時は-1返すとは限らない。

どちらかというとJMeterなどのクライアント側の注意点になるが、TcpNetServerConnectionFactory、かつ、single-use="false"の場合
Nettyを使わず素のSocketクライアント（not NIO）からのレスポンスのread()実行時にレスポンス終了時は-1が帰らずそのままダンマリになる。
シリアライザーの動きに合わせたレスポンス読み取りに挙動を揃えること。
例えば改行終わりなら改行検出時にレスポンス読み取りを打ち切ったりとか。
StxEtxならばEtx検出時にやめるようにするとか。
