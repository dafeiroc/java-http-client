# java http client for load testing

## jar ファイル作成方法

```shell
$ javac src/Main.java
$ cd src
$ jar cvf ../http-client.jar .
```

## 実行方法

```shell
$ java -cp http-client.jar Main ベースURL connectTimeout(ms) readTimeout(ms) スレッド数 リクエスト間のスリープタイム(ms) 1スレッドあたりの送信リクエスト数 1パラメータの長さ パラメータ数
```

例:

```shell
$ java -cp http-client.jar Main <your api url> 2000 2000 4 1000 20 16 1
```
