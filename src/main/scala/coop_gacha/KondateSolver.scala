package coop_gacha
import scala.math.pow
import coop_gacha.model.Menu

object GoodNutrition {
  val energy = 883.0f
  val protein = 21.7f
  val fat = 24.5f
  val carbohydrates = 127.0f
  val saltEquivalent = 2.5f
  val calcium = 266.7f
  val vegetableAmount = 116.7f
  val iron = 2.5f
  val vitaminA = 283.3f
  val vitaminB1 = 0.47f
  val vitaminB2 = 0.53f
  val vitaminC = 33.3f
}

object KondateSolver {
  trait SortMetric
  case object NutritionBalance extends SortMetric
  case object Energy extends SortMetric

  case class KondateConfig(
      val price: Int,
      val requireMainDish: Boolean,
      val allowDuplicateEntries: Boolean,
      val sortMetric: SortMetric,
      val priceRange: Int
  )

  // Copilot 万歳
  def nutritionScore(menus: Array[Menu]): Double = {
    (pow(
      (menus.map(_.energy).sum - GoodNutrition.energy) / GoodNutrition.energy,
      2
    ) +
      pow(
        (menus
          .map(_.protein)
          .sum - GoodNutrition.protein) / GoodNutrition.protein,
        2
      ) +
      pow((menus.map(_.fat).sum - GoodNutrition.fat) / GoodNutrition.fat, 2) +
      pow(
        (menus
          .map(_.carbohydrates)
          .sum - GoodNutrition.carbohydrates) / GoodNutrition.carbohydrates,
        2
      ) +
      pow(
        (menus
          .map(_.saltEquivalent)
          .sum - GoodNutrition.saltEquivalent) / GoodNutrition.saltEquivalent,
        2
      ) +
      pow(
        (menus
          .map(_.calcium)
          .sum - GoodNutrition.calcium) / GoodNutrition.calcium,
        2
      ) +
      pow(
        (menus
          .map(_.vegetableAmount)
          .sum - GoodNutrition.vegetableAmount) / GoodNutrition.vegetableAmount,
        2
      ) +
      pow(
        (menus.map(_.iron).sum - GoodNutrition.iron) / GoodNutrition.iron,
        2
      ) +
      pow(
        (menus
          .map(_.vitaminA)
          .sum - GoodNutrition.vitaminA) / GoodNutrition.vitaminA,
        2
      ) +
      pow(
        (menus
          .map(_.vitaminB1)
          .sum - GoodNutrition.vitaminB1) / GoodNutrition.vitaminB1,
        2
      ) +
      pow(
        (menus
          .map(_.vitaminB2)
          .sum - GoodNutrition.vitaminB2) / GoodNutrition.vitaminB2,
        2
      ) +
      pow(
        (menus
          .map(_.vitaminC)
          .sum - GoodNutrition.vitaminC) / GoodNutrition.vitaminC,
        2
      ))
  }
}

class KondateSolver(menus: Array[Menu], config: KondateSolver.KondateConfig) {
  private val random = new scala.util.Random()

  println(
    s"kondateSolver initialised with ${menus.length} menus and target ${config.price}"
  )
  lazy val menuChoices: Array[Array[Menu]] = {
    var dp: Array[Array[Array[Menu]]] =
      Array.fill(config.price + config.priceRange + 1)(Array.empty[Array[Menu]])
    for (menu <- menus) {
      var next_dp = if (config.allowDuplicateEntries) { dp }
      else { dp.clone() }
      if (menu.price < next_dp.length) {
        next_dp(menu.price) = next_dp(menu.price) :+ Array(menu)
      }
      for (price <- 0 until next_dp.length) {
        if (next_dp(price).nonEmpty) {
          val newPrice = price + menu.price
          if (newPrice < next_dp.length) {
            val existingMenus = if (dp(price).length > 1000) {
              println(
                s"Warning: dp at price $price exceeds 1000 entries: ${dp(price).length}"
              )
              for (i <- 0 until 1000) {
                val randIdx = random.nextInt(dp(price).length - i) + i
                val temp = dp(price)(i)
                dp(price)(i) = dp(price)(randIdx)
                dp(price)(randIdx) = temp
              }
              dp(price).slice(0, 1000)
            } else { dp(price) }
            next_dp(newPrice) = dp(newPrice) ++ existingMenus.map {
              existingMenu =>
                existingMenu :+ menu
            }
          }
        }
      }
      dp = next_dp
    }
    val result = dp
      .slice(
        config.price - config.priceRange,
        config.price + config.priceRange + 1
      )
      .flatten
    if (config.requireMainDish) {
      result.filter(_.exists(_.price > 200)) // 小鉢以外のおかずを含むように
    } else {
      result
    }
  }

  def gacha(): Array[Menu] = {
    menuChoices.length match {
      case 0 =>
        Array.empty[Menu]
      case _ =>
        val randomArea =
          random.nextInt(math.max(1, menuChoices.length - 300))
        val nutritionSorted = menuChoices
          .slice(
            randomArea,
            math.min(randomArea + 300, menuChoices.length)
          )
          .map { menus =>
            config.sortMetric match
              case KondateSolver.NutritionBalance =>
                (menus, KondateSolver.nutritionScore(menus))
              case KondateSolver.Energy => (menus, -menus.map(_.energy).sum)
          }
          .sortBy(_._2)
        nutritionSorted(
          math.min(nutritionSorted.length - 1, random.nextInt(5))
        )._1.sortBy(_.price)
    }
  }
}
