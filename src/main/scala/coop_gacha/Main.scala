package coop_gacha

import org.scalajs.dom
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.codecs.*
import scalajs.js
import scala.concurrent.Future
import scala.scalajs.js.annotation.JSName
implicit val ec: scala.concurrent.ExecutionContext =
  scala.concurrent.ExecutionContext.global

@main
def entry(): Unit = {
  println("Hello, Coop Gacha!")
  sealed trait KondateSolverState
  case object Ready extends KondateSolverState
  case object Solving extends KondateSolverState
  case object Solved extends KondateSolverState
  val kondateSolverState = Var[KondateSolverState](Ready)
  val availableMenus = Var(Option[Array[Menu]](null))
  val kondateSolver = Var(Option[KondateSolver](null))
  val solvedKondate = Var(Option[Array[Menu]](null))
  val kondateConfig = Var(
    KondateSolver.KondateConfig(
      price = 650,
      requireMainDish = true,
      allowDuplicateEntries = false,
      sortMetric = KondateSolver.NutritionBalance,
      priceRange = 20
    )
  )
  dom
    .fetch("./menu.json")
    .toFuture
    .flatMap(_.json().toFuture)
    .map { menusInfo =>
      class MenuID(
          @JSName("house")
          val houseId: String,
          @JSName("menu")
          val menuId: String
      ) extends js.Object
      class MenusPerWeek(
          val date: String,
          val menus: js.Array[MenuID]
      ) extends js.Object
      class MenusInfo(
          val menus_per_date: js.Array[MenusPerWeek],
          val menu_details: js.Array[Menu]
      ) extends js.Object

      val menuDetails = menusInfo.asInstanceOf[MenusInfo].menu_details.toArray
      val menusOfWeek = menusInfo
        .asInstanceOf[MenusInfo]
        .menus_per_date
        .sortBy(week => js.Date.parse(week.date))
        .reduceLeft((acc, week) => {
          if (js.Date.parse(week.date) > js.Date.now()) {
            acc // 現在より新しい週は無視; 現在より新しい週しかない場合は一番古い週が選択される
          } else {
            week
          }
        })
        .menus
        .toArray
        .map(menu =>
          menuDetails
            .find(m => m.houseId == menu.houseId && m.menuId == menu.menuId)
            .orNull
        )
        .filter(_ != null)
      availableMenus.set(Some(menusOfWeek))
      dom.window.location.hash match {
        case url if url != "" =>
          val menuIDs = js.URIUtils.decodeURIComponent(url.drop(1)).split(",")
          val menus = menuIDs
            .map(menu => {
              val parts = menu.split(":")
              if (parts.length == 2) {
                menuDetails
                  .find(m => m.houseId == parts(0) && m.menuId == parts(1))
                  .orNull
              } else {
                null
              }
            })
            .filter(_ != null)
            .toArray
          solvedKondate.set(Some(menus))
        case _ => {}
      }
    }
  kondateConfig.signal.foreach(_ => kondateSolver.set(None))(using
    unsafeWindowOwner
  )
  solvedKondate.signal.foreach(el =>
    el match {
      case Some(menus) =>
        val menusString =
          menus.map(menu => s"${menu.houseId}:${menu.menuId}").mkString(",")
        dom.window.location.hash = "#" + menusString
      case None => ()
    }
  )(using unsafeWindowOwner)
  kondateConfig.signal.foreach(_ => kondateSolver.set(None))(using
    unsafeWindowOwner
  )
  render(
    dom.document.getElementById("app"),
    div(
      h1(
        span("生協食堂"),
        span(
          input(
            idAttr := "kondate_price",
            typ := "text",
            inputMode := "numeric",
            pattern := "\\d*",
            value <-- kondateConfig.signal.map(_.price.toString),
            onInput.mapToValue
              .map(value =>
                value.toIntOption match {
                  case Some(price) =>
                    if (price >= 0 && price <= 1500) Some(price) else None
                  case None => if (value.isEmpty) Some(0) else None
                }
              )
              .map(_.getOrElse(kondateConfig.now().price).toString())
              .setAsValue --> { value =>
              kondateConfig.update(conf => conf.copy(price = value.toInt))
            }
          ),
          "円"
        ),
        span("ガチャ")
      ),
      p(
        input(
          idAttr := "kondate_price",
          typ := "text",
          inputMode := "numeric",
          pattern := "\\d*",
          width := "33px",
          value <-- kondateConfig.signal.map(_.price.toString),
          onInput.mapToValue
            .map(value =>
              value.toIntOption match {
                case Some(price) =>
                  if (price >= 0 && price <= 1500) Some(price) else None
                case None => if (value.isEmpty) Some(0) else None
              }
            )
            .map(_.getOrElse(kondateConfig.now().price).toString())
            .setAsValue --> { value =>
            kondateConfig.update(conf => conf.copy(price = value.toInt))
          }
        ),
        "±",
        input(
          typ := "text",
          inputMode := "numeric",
          pattern := "\\d*",
          fontSize := "inherit",
          width := "22px",
          textAlign.center,
          value <-- kondateConfig.signal.map(_.priceRange.toString),
          onInput.mapToValue
            .map(value =>
              value.toIntOption match {
                case Some(priceRange) =>
                  if (priceRange >= 0 && priceRange <= 100) Some(priceRange)
                  else None
                case None => if (value.isEmpty) Some(0) else None
              }
            )
            .map(_.getOrElse(kondateConfig.now().priceRange).toString())
            .setAsValue --> { value =>
            kondateConfig.update(conf => conf.copy(priceRange = value.toInt))
          }
        ),
        "円で、かつ",
        select(
          onChange.mapToValue --> { selected =>
            selected match {
              case "nutrition" =>
                kondateConfig.update(conf =>
                  conf.copy(sortMetric = KondateSolver.NutritionBalance)
                )
              case "energy" =>
                kondateConfig.update(conf =>
                  conf.copy(sortMetric = KondateSolver.Enery)
                )
            }
          },
          option(
            "栄養バランスを重視した",
            value := "nutrition",
            selected <-- kondateConfig.signal.map(
              _.sortMetric == KondateSolver.NutritionBalance
            )
          ),
          option(
            "カロリーを重視した",
            value := "energy",
            selected <-- kondateConfig.signal.map(
              _.sortMetric == KondateSolver.Enery
            )
          )
        ),
        "食事の組み合わせをランダムに生成します"
      ),
      div(
        idAttr := "control_panel",
        display.flex,
        flexWrap.wrap,
        alignItems.center,
        justifyContent.center,
        margin := "10px",
        gap := "10px",
        label(
          input(
            typ := "checkbox",
            checked <-- kondateConfig.signal.map(_.requireMainDish),
            onChange.mapToChecked --> { checked =>
              kondateConfig.update(conf => conf.copy(requireMainDish = checked))
            }
          ),
          "主菜(200円以上)を必ず含める"
        ),
        label(
          input(
            typ := "checkbox",
            checked <-- kondateConfig.signal.map(_.allowDuplicateEntries),
            onChange.mapToChecked --> { checked =>
              kondateConfig.update(conf =>
                conf.copy(allowDuplicateEntries = checked)
              )
            }
          ),
          "料理の重複を許可する(動作が重くなります)"
        )
      ),
      child <-- availableMenus.signal.map {
        case Some(menus) =>
          button(
            child <-- kondateSolverState.signal.map {
              case Ready   => "ガチャを回す"
              case Solving => "ガチャ中..."
              case Solved  => "もう一度"
            },
            onClick --> { _ =>
              kondateSolverState.set(Solving)
              if (kondateSolver.now().isEmpty) {
                kondateSolver.set(
                  Some(new KondateSolver(menus, kondateConfig.now()))
                )
              }
              kondateSolver.now() match {
                case Some(solver) =>
                  solvedKondate.set(Some(solver.gacha()))
                  kondateSolverState.set(Solved)
                case None => {}
              }
            }
          )
        case None =>
          div(
            p(
              "Loading kondateData..."
            )
          )
      },
      div(
        child <-- solvedKondate.signal.map {
          case Some(menus) =>
            div(
              hr(),
              h2(
                idAttr := "kondate_heading",
                s"今日の献立 (計${menus.map(_.price).sum}円)",
                button(
                  idAttr := "share",
                  i(
                    className := "ri-share-fill"
                  ),
                  onClick --> { _ =>
                    val text =
                      s"今日の献立:\n${menus.reverse.map(menu => s"${menu.name} (${menu.price}円)").mkString("\n")}"
                    js.Dynamic.global.window.navigator.share(
                      js.Dynamic.literal(
                        "text" -> text,
                        "url" -> dom.window.location.href
                      )
                    )
                  }
                )
              ),
              if (menus.nonEmpty) div(
                  idAttr := "kondate_list",
                  menus.map { menu =>
                    a(
                      href := s"https://west2-univ.jp/sp/detail.php?t=${menu.houseId}&c=${menu.menuId}",
                      target := "_blank",
                      rel := "noopener noreferrer",
                      img(
                        src := s"https://west2-univ.jp/menu_img/png_sp/${menu.menuId}.png",
                        alt := s"${menu.name}の画像"
                      ),
                      span(
                        span(menu.name),
                        span(s"${menu.price}")
                      )
                    )
                  }
                ) else div(
                  p("(トレイに何も載せずにレジに向かってください)")
                ),
              div(
                h2("栄養バランス"),
                span(
                  f"基準値とのずれの度合い: ${KondateSolver.nutritionScore(menus)}%.2f"
                ),
                table(
                  thead(
                    tr(
                      th("栄養素"),
                      th("合計"),
                      th("基準値(多分)")
                    )
                  ),
                  tbody(
                    tr(
                      td("エネルギー"),
                      td(f"${menus.map(_.energy).sum}%.1f kcal"),
                      td(f"${GoodNutrition.energy}%.1f kcal")
                    ),
                    tr(
                      td("たんぱく質"),
                      td(f"${menus.map(_.protein).sum}%.1f g"),
                      td(f"${GoodNutrition.protein}%.1f g")
                    ),
                    tr(
                      td("脂質"),
                      td(f"${menus.map(_.fat).sum}%.1f g"),
                      td(f"${GoodNutrition.fat}%.1f g")
                    ),
                    tr(
                      td("炭水化物"),
                      td(f"${menus.map(_.carbohydrates).sum}%.1f g"),
                      td(f"${GoodNutrition.carbohydrates}%.1f g")
                    ),
                    tr(
                      td("食塩相当量"),
                      td(f"${menus.map(_.saltEquivalent).sum}%.1f g"),
                      td(f"${GoodNutrition.saltEquivalent}%.1f g")
                    ),
                    tr(
                      td("カルシウム"),
                      td(f"${menus.map(_.calcium).sum}%.1f mg"),
                      td(f"${GoodNutrition.calcium}%.1f mg")
                    ),
                    tr(
                      td("野菜量"),
                      td(f"${menus.map(_.vegetableAmount).sum}%.1f g"),
                      td(f"${GoodNutrition.vegetableAmount}%.1f g")
                    ),
                    tr(
                      td("鉄"),
                      td(f"${menus.map(_.iron).sum}%.1f mg"),
                      td(f"${GoodNutrition.iron}%.1f mg")
                    ),
                    tr(
                      td("ビタミンA"),
                      td(f"${menus.map(_.vitaminA).sum}%.1f μg"),
                      td(f"${GoodNutrition.vitaminA}%.1f μg")
                    ),
                    tr(
                      td("ビタミンB1"),
                      td(f"${menus.map(_.vitaminB1).sum}%.1f mg"),
                      td(f"${GoodNutrition.vitaminB1}%.1f mg")
                    ),
                    tr(
                      td("ビタミンB2"),
                      td(f"${menus.map(_.vitaminB2).sum}%.1f mg"),
                      td(f"${GoodNutrition.vitaminB2}%.1f mg")
                    ),
                    tr(
                      td("ビタミンC"),
                      td(f"${menus.map(_.vitaminC).sum}%.1f mg"),
                      td(f"${GoodNutrition.vitaminC}%.1f mg")
                    )
                  )
                )
              )
            )
          case None => emptyNode
        }
      ),
      p(
        color := "gray",
        "Made with Scala.js and Laminar."
      )
    )
  )
}
