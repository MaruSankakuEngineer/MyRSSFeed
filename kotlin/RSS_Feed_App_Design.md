# Android RSSフィードリーダーアプリ設計書

## 1. プロジェクト概要

### 1.1 アプリ名
**MyRSSFeed Reader**

### 1.2 目的
- ホーム画面のウィジェットにRSSフィードを表示
- ウィジェット上に多数の記事を表示
- 記事をタップして直接ブラウザで開く
- 快適なウィジェット体験

### 1.3 ターゲットユーザー
- ホーム画面から素早く情報にアクセスしたいユーザー
- 複数のRSSフィードを一元管理したいユーザー
- ウィジェットベースのRSSリーダーを求めるユーザー

## 2. 技術仕様

### 2.1 開発環境
- **言語**: Kotlin
- **最小SDK**: API 24 (Android 7.0)
- **ターゲットSDK**: API 35 (Android 15)
- **アーキテクチャ**: MVVM + Repository Pattern
- **非同期処理**: Coroutines + Flow
- **UI**: XML Layout + Jetpack Compose（設定画面）

### 2.2 使用ライブラリ
```kotlin
// 既存ライブラリ
implementation(libs.androidx.core.ktx)
implementation(libs.androidx.appcompat)
implementation(libs.material)
implementation(libs.androidx.activity)
implementation(libs.androidx.constraintlayout)

// 追加予定ライブラリ
implementation("androidx.compose.ui:ui:1.6.0")
implementation("androidx.compose.material3:material3:1.2.0")
implementation("androidx.compose.ui:ui-tooling-preview:1.6.0")
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
implementation("androidx.navigation:navigation-compose:2.7.7")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-simplexml:2.9.0")
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
implementation("io.coil-kt:coil:2.5.0")
implementation("androidx.work:work-runtime-ktx:2.9.0")
implementation("androidx.appwidget:appwidget:1.0.0")
```

## 3. アーキテクチャ設計

### 3.1 パッケージ構造
```
com.example.myrssfeed/
├── data/
│   ├── local/
│   │   ├── dao/
│   │   ├── entity/
│   │   └── database/
│   ├── remote/
│   │   ├── api/
│   │   ├── dto/
│   │   └── service/
│   └── repository/
├── domain/
│   ├── model/
│   ├── repository/
│   └── usecase/
├── presentation/
│   ├── ui/
│   │   ├── main/
│   │   ├── settings/
│   │   └── components/
│   ├── viewmodel/
│   └── theme/
├── widget/
│   ├── RssWidgetProvider.kt
│   ├── RssWidgetService.kt
│   └── widget_layouts/
├── util/
│   ├── Constants.kt
│   ├── Extensions.kt
│   └── DateUtils.kt
└── di/
    └── AppModule.kt
```

### 3.2 データフロー
```
UI → ViewModel → UseCase → Repository → DataSource
   ↑                                                ↓
   ←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←
```

## 4. 機能仕様

### 4.1 RSSウィジェット
- **ホーム画面配置可能**
- **複数サイズ対応**: 2x2, 3x2, 4x2, 4x4
- **表示件数**: サイズに応じて3-15件表示
- **表示内容**:
  - 記事タイトル（最大2行）
  - フィード名
  - 公開日時
  - サムネイル画像（あれば）
- **機能**:
  - 記事タップでブラウザで直接開く
  - ヘッダーフィルターでサイト選択
  - 長押しでウィジェット設定
  - 自動更新（設定可能な間隔）

### 4.2 設定画面
- RSSフィード管理（追加・編集・削除）
- ウィジェット表示設定（記事数、画像表示ON/OFF）
- 更新間隔設定（15分、30分、1時間、3時間、6時間、12時間、24時間）
- テーマ設定（ダーク/ライト/システム）
- データ管理（キャッシュクリア、エクスポート）

### 4.3 オフライン対応
- 記事のローカルキャッシュ
- オフライン時の既読記事表示
- ネットワーク復旧時の自動同期

## 5. UI/UX設計

### 5.1 デザイン原則
- **Material Design 3**準拠
- **コンパクトなウィジェット表示**
- **直感的なタップ操作**
- **情報密度の最適化**

### 5.2 ウィジェットレイアウト
```
┌─────────────────────────────────┐
│ 📰 RSSフィード名 [フィルター▼]  │ ← ヘッダー（フィルター付き）
├─────────────────────────────────┤
│ 記事タイトル1                   │
│ フィード名 • 1時間前            │
├─────────────────────────────────┤
│ 記事タイトル2                   │
│ フィード名 • 2時間前            │
├─────────────────────────────────┤
│ 記事タイトル3                   │
│ フィード名 • 3時間前            │
├─────────────────────────────────┤
│ 記事タイトル4                   │
│ フィード名 • 4時間前            │
└─────────────────────────────────┘
```

**フィルター機能**:
- ヘッダーのフィルターボタンをタップでフィルター選択
- 「すべて」「サイトA」「サイトB」「サイトC」など
- 選択したサイトの記事のみ表示
- フィルター状態はウィジェットごとに保存

### 5.3 表示最適化
- **コンパクトなレイアウト**による情報密度向上
- **画像の効率的な表示**
- **テキストの省略表示**
- **タップ領域の最適化**

## 6. データモデル

### 6.1 エンティティ
```kotlin
// RSSフィード
@Entity(tableName = "rss_feeds")
data class RssFeed(
    @PrimaryKey val id: String,
    val title: String,
    val url: String,
    val description: String?,
    val lastUpdated: Long,
    val updateInterval: Int,
    val isEnabled: Boolean = true,
    val sortOrder: Int = 0,
    val category: String? = null // フィルター用カテゴリ
)

// RSS記事
@Entity(
    tableName = "rss_articles",
    indices = [Index("feedId"), Index("publishedAt")]
)
data class RssArticle(
    @PrimaryKey val id: String,
    val feedId: String,
    val title: String,
    val description: String?,
    val content: String?,
    val link: String,
    val publishedAt: Long,
    val imageUrl: String?,
    val isRead: Boolean = false,
    val isBookmarked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

// ブックマーク
@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey val articleId: String,
    val createdAt: Long = System.currentTimeMillis()
)

// ウィジェット設定
@Entity(tableName = "widget_settings")
data class WidgetSettings(
    @PrimaryKey val widgetId: Int,
    val selectedFeedId: String? = null, // 選択されたフィードID（null=すべて）
    val displayCount: Int = 5,
    val showImages: Boolean = true,
    val theme: String = "system" // light, dark, system
)
```

### 6.2 DTO
```kotlin
// RSS XMLレスポンス
data class RssResponse(
    val channel: RssChannel
)

data class RssChannel(
    val title: String,
    val description: String?,
    val items: List<RssItem>
)

data class RssItem(
    val title: String,
    val description: String?,
    val link: String,
    val pubDate: String,
    val enclosure: RssEnclosure?
)

data class RssEnclosure(
    val url: String,
    val type: String?
)
```

## 7. パフォーマンス最適化

### 7.1 ウィジェット最適化
- **軽量なデータ構造**による高速表示
- **Room Database**の最適化
- **画像キャッシュ**（Coil使用）
- **メモリ効率的なウィジェット表示**

### 7.2 表示件数
- **2x2サイズ**: 3-5件表示
- **3x2サイズ**: 5-8件表示
- **4x2サイズ**: 8-12件表示
- **4x4サイズ**: 12-15件表示
- **キャッシュ**: 最新100件をローカル保存

### 7.3 更新戦略
- **バックグラウンド更新**: 設定可能な間隔
- **ウィジェット更新**: 自動更新
- **差分更新**: 新着記事のみ取得
- **重複排除**: URLベースの重複チェック


## 8. 実装計画

### 8.1 Phase 1: 基盤構築
1. データベース設計と実装
2. ネットワーク層の実装
3. 基本的なRepository実装
4. WorkManager設定

### 8.2 Phase 2: ウィジェット実装
1. WidgetProviderの実装
2. ウィジェットレイアウト作成
3. データ表示機能
4. タップ機能実装
5. ヘッダーフィルター機能実装

### 8.3 Phase 3: 設定機能
1. 設定画面の実装
2. フィード管理機能
3. 設定の永続化
4. テーマ設定

### 8.4 Phase 4: 最適化
1. パフォーマンス最適化
2. オフライン対応
3. エラーハンドリング
4. 最終調整

## 9. 追加機能案

### 9.1 ウィジェット機能
- **複数ウィジェット**対応
- **ウィジェットサイズ**自動調整
- **カスタムテーマ**設定
- **更新頻度**カスタマイズ
- **フィルター機能**強化
  - カテゴリ別フィルター
  - 複数フィード選択
  - フィルター履歴保存

### 9.2 フィルター機能
- **サイト別**フィルター（ヘッダーから選択）
- **カテゴリ別**フィルター
- **日付範囲**フィルター
- **キーワード**フィルター
- **重要度**フィルター

### 9.3 カスタマイズ機能
- **フォントサイズ**調整
- **行間**調整
- **カラーテーマ**カスタマイズ
- **レイアウト**カスタマイズ

## 10. セキュリティ・プライバシー

### 10.1 ネットワークセキュリティ
- HTTPS通信の強制
- 証明書ピニング（必要に応じて）
- ネットワークセキュリティ設定

### 10.2 データ保護
- ローカルデータの暗号化
- 機密情報の安全な保存
- プライバシーポリシーの提供

## 11. パフォーマンス要件

### 11.1 応答性
- ウィジェット更新: 3秒以内
- 記事タップ応答: 1秒以内
- アプリ起動: 2秒以内
- 設定画面遷移: 1秒以内

### 11.2 リソース使用量
- メモリ使用量: 100MB以下
- バッテリー消費: 最小限
- ネットワーク使用量: 効率的な更新

## 12. テスト戦略

### 12.1 テスト種類
- **ユニットテスト**: ViewModel, Repository, UseCase
- **統合テスト**: データベース操作, ネットワーク通信
- **ウィジェットテスト**: 表示, 更新, タップ操作
- **手動テスト**: 各種デバイスでの動作確認

### 12.2 テストカバレッジ目標
- コードカバレッジ: 80%以上
- 重要なビジネスロジック: 100%

---

**作成日**: 2024年12月
**バージョン**: 1.0
**作成者**: AI Assistant 