# MyRSSFeed

Android用のRSSフィードリーダーアプリです。ホーム画面ウィジェットでRSS記事を表示できます。

## 機能

- RSSフィードの追加・管理
- ホーム画面ウィジェットでの記事表示
- 複数のRSS形式対応（RSS 2.0、RDF）
- 記事の既読管理
- フィード別フィルタリング

## 技術スタック

- **言語**: Kotlin
- **UI**: XML Layouts + RemoteViews (Widget)
- **アーキテクチャ**: Repository Pattern
- **データベース**: Room
- **ネットワーク**: Retrofit + SimpleXML
- **非同期処理**: Kotlin Coroutines

## 対応RSS形式

- RSS 2.0
- RDF (RSS 1.0)

## ウィジェット機能

- 複数サイズ対応
- 記事クリックでブラウザ起動
- フィード別フィルタリング
- 自動更新

## セットアップ

1. Android Studioでプロジェクトを開く
2. Gradle同期を実行
3. アプリをビルド・実行

## 使用方法

1. アプリを起動
2. RSSフィードURLを追加
3. ホーム画面にウィジェットを配置
4. 記事をタップしてブラウザで開く

## ライセンス

MIT License 