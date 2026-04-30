/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.timing

import net.ccbluex.liquidbounce.utils.extensions.safeDiv
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextInt
import net.minecraft.util.MathHelper
import java.security.SecureRandom
import java.util.*
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object TimeUtils {
    fun randomDelay(minDelay: Int, maxDelay: Int) = nextInt(minDelay, maxDelay + 1)
    fun getInRange(min: Int, max: Int) = Random().nextInt(max - min + 1) + min

    fun getCustomClickDelay(): Int {
        return ((if (getInRange(1, 7) == 1) getInRange(50, 74) else (if (getInRange(
                1,
                7
            ) <= 2
        ) 87 else getInRange(84, 89))))
    }

    fun getCustomBFDelay(): Int {
        return if (getInRange(1, 10) == 1) getInRange(225, 250) else if (getInRange(
                1,
                6
            ) == 1
        ) getInRange(89, 94) else if (getInRange(1, 3) == 1) getInRange(
            95,
            103
        ) else if (getInRange(1, 3) == 1) getInRange(115, 123) else if (getInRange(
                1,
                2
            ) == 1
        ) getInRange(131, 136) else getInRange(165, 174)
    }
    fun randomClickDelay(minCPS: Int, maxCPS: Int, mode: String): Int {
        when(mode) {
            "Record1"->return getRecordDataOf1()
            "Record2"->return getRecordDataOf2()
            "Record3"->return getRecordDataOf3()
            "Extra"-> return (1000.0 / getExtraRandomization( getInRange(minCPS, maxCPS).toDouble(), 1.5)).toInt()
            "Jitter"-> return (1000.0 / getPattern1Randomization(getInRange(minCPS, maxCPS).toDouble(), 1.5)).toInt()
            "ConstGenerate"-> return getCustomBFDelay()
            "ConstGenerate2"-> return getCustomClickDelay()
            "Butterfly"-> return (1000.0 / getPattern2Randomization(getInRange(minCPS, maxCPS).toDouble(), 1.5)).toInt()
            "ConstantMiddle"-> return (1000.0 / ((minCPS+maxCPS)/2)).toInt()
        }
        //basic RNG
        val minDelay = 1000 safeDiv minCPS
        val maxDelay = 1000 safeDiv maxCPS
        return (Math.random() * (minDelay - maxDelay) + maxDelay).roundToInt()
    }

    var patternIndex: Int = 0

    var devRand1: Double = 0.0

    var devRand2: Double = 0.0

    var devRand3: Double = 0.0

    var devRand4: Double = 0.0

    var devRandTick: Int = 0

    val recordedTimings1: IntArray = intArrayOf(
        134, 16, 75, 126, 22, 76, 119, 21, 94, 120,
        32, 56, 119, 27, 80, 151, 68, 134, 30, 68,
        115, 25, 92, 128, 28, 78, 134, 28, 71, 133,
        33, 50, 136, 32, 65, 137, 26, 63, 143, 23,
        78, 134, 29, 71, 136, 32, 68, 146, 31, 62,
        138, 165, 17, 67, 97, 25, 78, 100, 34, 67,
        32, 68, 116, 105, 28, 53, 105, 32, 79, 115,
        25, 61, 103, 24, 60, 109, 29, 74, 16, 98,
        18, 97, 18, 69, 25, 85, 110, 30, 82, 83,
        26, 83, 124, 22, 62, 134, 20, 77, 92, 27,
        88, 112, 23, 89, 122, 33, 67, 19, 88, 30,
        79, 134, 108, 108, 27, 78, 128, 23, 76, 134,
        27, 66, 140, 25, 79, 120, 44, 62, 124, 31,
        82, 108, 41, 85, 117, 27, 93, 129, 19, 76,
        136, 110, 142, 20, 67, 112, 34, 71, 114, 23,
        93, 105, 47, 58, 112, 27, 66, 117, 22, 108,
        111, 29, 83, 123, 27, 90, 127, 22, 92, 134,
        26, 93, 131, 34, 83, 115, 24, 94, 119, 47,
        73, 113, 30, 92, 115, 27, 92, 128, 30, 72,
        125, 115, 128, 30, 66, 135, 29, 57, 127, 106,
        145, 18, 68, 162, 70, 131, 21, 70, 127, 23,
        61, 148, 86, 105, 30, 95, 102, 32, 82, 125,
        42, 50, 150, 100, 124, 29, 64
    )


    val recordedTimings2: IntArray = intArrayOf(
        109, 76, 84, 177, 82, 110, 88, 83, 90, 110,
        63, 87, 83, 74, 93, 83, 100, 73, 72, 122,
        83, 117, 83, 84, 88, 82, 87, 78, 115, 80,
        86, 97, 87, 92, 92, 93, 93, 95, 87, 97,
        68, 108, 70, 68, 87, 71, 94, 67, 96, 75,
        81, 81, 93, 141, 87, 78, 96, 80, 91, 121,
        78, 96, 88, 132, 73, 92, 83, 95, 155, 89,
        88, 76, 85, 95, 88, 75, 83, 73, 90, 79,
        125, 89, 94, 150, 103, 71, 78, 98, 167, 77,
        103, 87, 84, 82, 88, 96, 166, 95, 67, 83,
        83, 67, 83, 78, 105, 73, 94, 99, 72, 93,
        85, 84, 100, 86, 83, 100, 67, 83, 85, 85,
        98, 65, 66, 84, 84, 99, 67, 101, 83, 82,
        117, 116, 84, 66, 83, 101, 67, 83, 168, 83,
        65, 134, 50, 84, 82, 84, 83, 83, 101, 99,
        83, 102, 65, 67, 68, 66, 83, 67, 152, 128,
        68, 79, 76, 93, 74, 100, 88, 71, 75, 93,
        72, 70, 83, 100, 84, 65, 96, 88, 71, 78,
        84, 84, 68, 87, 157, 65, 88, 68, 97, 68,
        113, 57, 93, 83, 72, 69, 78, 67, 84, 67,
        151, 100, 83, 83, 67, 91, 161, 72, 73, 100,
        84, 87, 95, 87, 80, 83, 67, 83, 67, 93,
        90, 84, 72, 94, 125, 81, 111, 83, 70, 80,
        153, 91, 73, 100, 83, 186
    )
    var recordTimeUse3 = intArrayOf(
        17, 79, 83, 24, 81, 86, 30, 67, 97, 25,
        79, 101, 33, 68, 129, 69, 96, 24, 77, 90,
        27, 81, 104, 30, 68, 94, 29, 80, 96, 25,
        72, 114, 25, 79, 100, 26, 75, 117, 22, 77,
    )
    var recordTimeUse2 = intArrayOf(52, 87, 74, 73, 80, 78, 88, 78, 72, 93,
        88, 72, 74, 76, 93, 77, 96, 68, 82, 75,
        73, 76, 90, 177, 66, 74, 76, 91, 86, 88,
        90, 94, 84, 85, 80, 83, 147, 103, 93, 80)
    var recordTimeUse =
        intArrayOf(107, 96, 46, 88, 80, 65, 94, 191, 60, 101, 91, 47, 105, 98, 111, 45, 106, 52, 96, 46)

    var idx1=0
    var idx2=0
    var idx3=0
    fun getRecordDataOf1(): Int {
        return if (++idx1 >= recordTimeUse.size) {
            idx1 = 0
            recordTimeUse[idx1]
        } else recordTimeUse[idx1]
    }
    fun getRecordDataOf2(): Int {
        return if (++idx2 >= recordTimeUse2.size) {
            idx2 = 0
            recordTimeUse2[idx2]
        } else recordTimeUse2[idx2]
    }
    fun getRecordDataOf3(): Int {
        return if (++idx3 >= recordTimeUse3.size) {
            idx3 = 0
            recordTimeUse3[idx3]
        } else recordTimeUse3[idx3]
    }

    fun getOldRandomization(cps: Double, sigma: Double): Double {
        val rnd = MathHelper.getRandomDoubleInRange(Random(), 0.0, 1.0)
        val normal = sqrt(-2.0 * ln(rnd) / ln(Math.E)) * sin(6.283185307179586 * rnd)
        return cps + sigma * normal
    }

    fun getNewRandomization(cps: Double, rand: Double): Double {
        val rnd = MathHelper.getRandomDoubleInRange(Random(), 0.0, 1.0)
        val normal = sqrt(-2.0 * ln(rnd) / ln(Math.E)) * sin(6.283185307179586 * rnd)
        return cps + rand * normal + (cps + (SecureRandom()).nextDouble() * rand) / 4.0
    }

    fun getExtraRandomization(cps: Double, rand: Double): Double {
        if (++devRandTick % 30 == 0) {
            devRand1 = MathHelper.getRandomDoubleInRange(Random(), 0.0, 1.0)
            devRand2 = MathHelper.getRandomDoubleInRange(Random(), 0.0, 1.0)
            devRand3 = MathHelper.getRandomDoubleInRange(Random(), 0.0, 1.0)
            devRand4 = MathHelper.getRandomDoubleInRange(Random(), 0.0, 1.0)
        }
        var randOffset1 = 0.0
        var randOffset2 = 0.0
        when (MathHelper.getRandomIntegerInRange(Random(), 1, 4)) {
            1 -> randOffset1 = devRand1
            2 -> randOffset1 = devRand2
            3 -> randOffset1 = devRand3
            4 -> randOffset1 = devRand4
        }
        when (MathHelper.getRandomIntegerInRange(Random(), 1, 4)) {
            1 -> randOffset2 = devRand1
            2 -> randOffset2 = devRand2
            3 -> randOffset2 = devRand3
            4 -> randOffset2 = devRand4
        }
        val rand1 = getNewRandomization(cps + (-0.3 + randOffset1 * 0.6) * rand, rand * (0.5 + randOffset1 * 0.3))
        val rand2 = getOldRandomization(cps + randOffset2 * rand, rand * (0.2 + randOffset2 * 0.4))
        return (3.0 * rand1 + rand2) / 4.0
    }

    fun getPattern1Randomization(cps: Double, rand: Double): Double {
        if (++patternIndex >= recordedTimings1.size) patternIndex = 0
        return cps + rand * recordedTimings1[patternIndex] / 164.0
    }

    fun getPattern2Randomization(cps: Double, rand: Double): Double {
        if (++patternIndex >= recordedTimings2.size) patternIndex = 0
        return cps + rand * recordedTimings2[patternIndex] / 174.0
    }
}