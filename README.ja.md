[English](README.md) | 日本語

# Copycatfix

Minecraft 1.20.1 用の小さな [Minecraft Forge](https://files.minecraftforge.net/) mixin パッチ mod で、
[Copycats+](https://modrinth.com/mod/copycats) のバグを修正します(`3.0.7+mc1.20.1` を対象にテスト済み)。
Create は `6.0.7` / `6.0.8` で動作確認しています。

## バグについて

Copycat の Panel(パネル)および Sliding Door(引き戸)ブロックは、素材(Material)を挿入した状態で
スケマティック経由で配置すると、その素材が失われ(空の Copycat Base に戻る)ます — 手動でスケマティックを印刷した場合
(スケマティックを持ち、ゴースト配置で右クリックしてクリエイティブで「print」)でも、
Schematicannon から発射した場合でも同様です。

他の Copycat タイプ(Beam、Slab、Step、Ladder、…)には影響しません。

## 根本原因

Copycats+ の `ICopycatBlockEntity#read` において、保存された `Material` タグが正しく解析された後に、
次のような再検証が行われます:

```java
ICopycatBlock#getAcceptedBlockState(level, pos, consumedItem, null)
```

これが失敗すると、素材(material)と消費されたアイテム(consumed item)が黙って空(`AllBlocks.COPYCAT_BASE`)に
リセットされます — しかし `Material` タグ自体は数行前に正しく読み取られています。

スケマティックで配置された Panel / Sliding Door ブロックの場合、読み込み時に `consumedItem`(どのアイテムで
コピーキャットを埋めたかを記録する "Item" NBT タグ)が空になることがあります。
空のアイテム(`ItemStack.EMPTY.getItem()` が `BlockItem` ではない)に対して `getAcceptedBlockState` は
すぐに `null` を返すため、再検証は常に失敗し、正当な保存済み素材が消去されてしまいます。

## 修正内容

この mod は `ICopycatBlockEntity#read` をターゲットとする単一の Mixin を提供し、破壊的な再検証リセット処理を
削除します。一度読み取った `Material` タグは信頼され、他のすべての Copycat タイプと同様に扱われます。
動作はそれ以外変更されず、Copycats+ 自体は修正・置換されません — これは単独で追加されるパッチ mod です。

実装と理由の全容は
[`ICopycatBlockEntityMixin.java`](src/main/java/example/copycatfix/mixin/ICopycatBlockEntityMixin.java)
を参照してください。

## 動作環境(Requirements)

- Minecraft **1.20.1**
- Minecraft Forge **47.4.21**(または互換の `[47,)`)
- [Create](https://modrinth.com/mod/create) **6.0.7+**
- [Copycats+](https://modrinth.com/mod/copycats) **3.0.7+**

## インストール方法

1. 最新の `copycatfix-*.jar` を [Releases](https://github.com/harukimaru2013-design/copy-cat-fix/releases)
   ページからダウンロードします。
2. Create と Copycats+ と同じ `mods` フォルダに jar を入れてください(Copycats+ の jar を置き換えたり
   変更したりしないでください — この mod は上書きせず併用します)。
3. これはワールド / スケマティックの読み込み時に動作するデータ整合性の修正なので、クライアントとサーバー
   両方にインストールしてください(シングルプレイでも同様です)。

## ソースからビルドする

```
git clone https://github.com/harukimaru2013-design/copy-cat-fix.git
cd copy-cat-fix
./gradlew build
```

ビルドされた jar は `build/libs/copycatfix-<version>.jar` に出力されます。

## ライセンス

[MIT](LICENSE)
