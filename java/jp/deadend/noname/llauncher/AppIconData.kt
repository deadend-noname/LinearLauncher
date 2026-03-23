package jp.deadend.noname.llauncher

data class AppIconData(val packageName: String)
// Stringのままにすると、Coilが自動的にUriに変換するのでFetcherの作成に失敗するらしい