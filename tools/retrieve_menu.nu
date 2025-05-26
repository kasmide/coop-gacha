let HOUSE_ID = 663151
let menus_per_date = curl $"https://west2-univ.jp/sp/menu.php?t=($HOUSE_ID)"
  | lines
  | parse -r r#'id="(?P<bunrui>.+?)">(?P<date>\d+年\d+月\d+日)'#
  | each {{date: ($in.date|into datetime --format "%Y年%m月%d日" -z JST | format date "%+"), bunrui: $in.bunrui}}
  | each {{date: $in.date, menus:
    (curl https://west2-univ.jp/sp/menu_load.php?t=($HOUSE_ID)&a=($in.bunrui)
    | parse -r r#'detail.php\?t=(?P<house>\d+)&c=(?P<menu>[0-9_]+)'#)
  }}

let existing_menus = try { open menu.json | get menu_details } catch { [] }
let menus = $menus_per_date | get menus | flatten | uniq
  | each { |menu_id|
    $existing_menus | where { |details|
      $details.食堂ID == $menu_id.house and $details.メニュー == $menu_id.menu
    } | get -i 0 | if $in != null { $in } else (do {
      let name = (
        sleep 500ms;
        curl $"https://west2-univ.jp/sp/get1menu.php?t=($menu_id.house)&c=($menu_id.menu)"
        | parse -r r#'<h3>(.+?)<span>'#
        | get 0.capture0
      )
      let nutrition = (
        sleep 500ms;
        curl $'https://west2-univ.jp/sp/get1total.php?t=($menu_id.house)' -H $'Cookie: west2tray($menu_id.house)=%2F($menu_id.menu)'
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
      {
        食堂ID: $menu_id.house,
        メニュー: $menu_id.menu,
        名前: $name,
        ...$nutrition,
      }
    })
  }

{
  menus_per_date: $menus_per_date,
  menu_details: $menus,
}
| to json -r | save -f menu.json