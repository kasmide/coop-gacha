let HOUSE_ID = 663151
let menus_per_date = curl $"https://west2-univ.jp/sp/menu.php?t=($HOUSE_ID)"
  | lines
  | parse -r r#'id="(?P<bunrui>.+?)">(?P<date>\d+年\d+月\d+日)'#
  | each {{date: ($in.date|into datetime --format "%Y年%m月%d日" -z JST | format date "%+"), bunrui: $in.bunrui}}
  | each {{date: $in.date, menus:
    (curl https://west2-univ.jp/sp/menu_load.php?t=($HOUSE_ID)&a=($in.bunrui)
    | parse -r r#'detail.php\?t=(?P<house>\d+)&c=(?P<menu>[0-9_]+)'#)
  }}

let nutrition = $menus_per_date | get menus | flatten | uniq | each {
    {
        ...$in,
        ...(
          sleep 500ms;
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

let menus = $nutrition
| each {
  {
    食堂ID: $in.house,
    メニュー: $in.menu,
    名前: (
      sleep 500ms;
      curl $"https://west2-univ.jp/sp/get1menu.php?t=($in.house)&c=($in.menu)"
      | parse -r r#'<h3>(.+?)<span>'#
      |get 0.capture0),
    ,
    ...($in | reject house menu)
  }
}

{
  menus_per_date: $menus_per_date,
  menu_details: $menus,
}
| to json -r | save -f menu.json