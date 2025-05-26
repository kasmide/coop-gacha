package coop_gacha.model

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

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

class Menu(
    @JSName("名前") val name: String,
    @JSName("食堂ID") val houseId: String,
    @JSName("メニュー") val menuId: String,
    @JSName("組価(税込)") val price: Int,
    @JSName("エネルギー") val energy: Double,
    @JSName("タンパク質") val protein: Double,
    @JSName("脂質") val fat: Double,
    @JSName("炭水化物") val carbohydrates: Double,
    @JSName("食塩相当量") val saltEquivalent: Double,
    @JSName("カルシウム") val calcium: Double,
    @JSName("野菜量") val vegetableAmount: Double,
    @JSName("鉄") val iron: Double,
    @JSName("ビタミン A") val vitaminA: Double,
    @JSName("ビタミン B1") val vitaminB1: Double,
    @JSName("ビタミン B2") val vitaminB2: Double,
    @JSName("ビタミン C") val vitaminC: Double
) extends js.Object