let nutrition = curl https://west2-univ.jp/sp/menu_load.php?t=663151&a=on_bunrui4 # TODO: 分類の規則性を調べてちゃんと自動化する
  | parse -r r#'detail.php\?t=(?P<house>\d+)&c=(?P<menu>[0-9_]+)'#
  | each {
    {
        ...$in,
        ...(
          curl $'https://west2-univ.jp/sp/get1total.php?t=($in.house)' -H $'Cookie: west2tray($in.house)=%2F($in.menu)'
          | tr -d "\\r\\n"
          | parse -r r#'<li>(.+?)</li>'#
          | get capture0
          | each {
            {
              ($in
              | parse -r r#'<strong>(?P<entity>.+?)</strong>'#
              | get 0.entity
              ): ($in
              | parse -r r#'<span class="price">.*?([0-9\.]+).*</span>'#
              | get 0.capture0
              | into float)
              }
            }
            | reduce {|elm, acc| $acc|merge $elm}
        )
    }
  }

$nutrition
| each {
  {
    食堂ID: $in.house,
    メニュー: $in.menu,
    名前: (
      curl $"https://west2-univ.jp/sp/get1menu.php?t=($in.house)&c=($in.menu)"
      | parse -r r#'<h3>(.+?)<span>'#
      |get 0.capture0),
    ,
    ...($in | reject house menu)
  }
}
| to json | save -f menu.json