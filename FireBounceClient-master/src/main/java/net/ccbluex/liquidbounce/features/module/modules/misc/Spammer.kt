/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import kotlinx.coroutines.delay
import net.ccbluex.liquidbounce.FireBounce.CLIENT_NAME
import net.ccbluex.liquidbounce.event.async.loopSequence
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextFloat
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextInt
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.randomString

object Spammer : Module("Spammer", Category.MISC, subjective = true) {

    private val delay by intRange("Delay", 500..1000, 0..5000)

    private val mode by choices("Mode", arrayOf("EmoThings","Joke", "Client", "Hello?", "Custom"), "Custom")

    private val message by text("Message", "$CLIENT_NAME Client | liquidbounce(.net) | FireFly on yt")

    private val custom by boolean("Custom", false)
    private val randomLength by intRange("RandomLength",5..11,0..30) {!custom}

    private var i = 0

    val onUpdate = loopSequence {
        var finalMsg = if (custom) replace(message)
        else message + " [" + randomString(nextInt(randomLength.first, randomLength.last)) + "]"
        when(mode){
            "Joke" -> {
                if(i< jokes.size) {
                    finalMsg = jokes[i]
                    i++
                }
                else i = 0
            }
            "Client"-> {
                if(i< c.size) {
                    finalMsg = c[i]
                    i++
                }
                else i = 0
            }
            "EmoThings"-> {
                if(i< wtfIsThis.size) {
                    finalMsg = "%p "+wtfIsThis[i]
                    i++
                }
                else i = 0
            }
            "Hello?"-> finalMsg="%p Hello!"
        }
        mc.thePlayer?.sendChatMessage(
            replace(finalMsg)
        )

        delay(delay.random().toLong())
    }

    private fun replace(text: String): String {
        var replacedStr = text

        replaceMap.forEach { (key, valueFunc) ->
            replacedStr = replacedStr.replace(key, valueFunc)
        }

        return replacedStr
    }

    private inline fun String.replace(oldValue: String, newValueProvider: () -> Any): String {
        var index = 0
        val newString = StringBuilder(this)
        while (true) {
            index = newString.indexOf(oldValue, startIndex = index)
            if (index == -1) {
                break
            }

            // You have to replace them one by one, otherwise all parameters like %s would be set to the same random string.
            val newValue = newValueProvider().toString()
            newString.replace(index, index + oldValue.length, newValue)

            index += newValue.length
        }
        return newString.toString()
    }

    private fun randomPlayer() =
        mc.netHandler.playerInfoMap
            .map { playerInfo -> playerInfo.gameProfile.name }
            .filter { name -> name != mc.thePlayer.name }
            .randomOrNull() ?: "none"

    private val replaceMap = mapOf(
        "%f" to { nextFloat().toString() },
        "%i" to { nextInt(0, 10000).toString() },
        "%ss" to { randomString(nextInt(1, 6)) },
        "%s" to { randomString(nextInt(1, 10)) },
        "%ls" to { randomString(nextInt(1, 17)) },
        "%p" to { randomPlayer() }
    )

    val wtfIsThis = arrayOf(
            "距离之所以可怕，因为根本不知道对方是把你想念，还是把你忘记。",
            "我不知道自己到底在执着什么，但我知道，我一直都在为难自己。",
            "有些事情不是看到希望才去坚持，而是坚持了才看得到希望！",
            "生命那么短，世界那么乱，我不想争吵，不想冷战，不愿和你有一秒遗憾。",
            "如果你能解释为什么会喜欢一个人，那么这不是爱情，真正的爱情没有原因，你爱他，不知道为什么。",
            "地球之所以是圆的，是因为上帝想让那些走失或迷路的人重新相遇。",
            "能让你生气的敌人，说明你没有胜他的把握；能让你生气的朋友，说明你仍在意他的友情。",
            "翅膀长在你的肩上，太在乎别人对于飞行姿势的批评，所以你飞不起来。",
            "那些刻在椅背后的爱情会不会像水泥地上的花朵，开出地老天荒的，没有风的森林。",
            "一句顺其自然，里面包含了我多少绝望和不甘心，如果你懂。",
            "我总是这样凝望那些日升月沉无家可归的忧伤。",
            "少走了弯路，也就错过了风景，无论如何，感谢经历。",
            "是你的，永远都是你的；不是你的，不管你怎么争，怎么抢，也都不会属于你。",
            "生活总会给你答案，但不会马上把一切都告诉你。",
            "与其在别人的故事里留着自己的泪，不如在自己的故事里笑得很大声。",
            "一个男人的强大，不在于他能摧毁什么，而在于他能保护什么！",
            "我总是在最深的绝望里，看见最美的风景。",
            "人生短短数十载，最要紧的是满足自己，不是讨好他人。",
            "宁愿花时间去修炼不完美的自己，也不要浪费时间去期待完美的别人。",
            "来到这个世界的理由，是因为世上有那么一件事，只有我才能做到。",
            "所谓爱情，就是一个人相信了另一个人的所有谎言。",
            "我们之间的陌生是我们永远也无法跨越的距离。",
            "我是多么想我们和好，可现实总是残酷的。",
            "生活有别于小说，悲剧不能成就美感。",
            "时间已覆水难收，梦醒就不再拥有。",
            "选择逃避，是你最好的办法，那么我选择离开。",
            "当你分开之后，我们就不在属于统一个世界。",
            "如果没有如果，曾经已是曾经，过去早已过去，做自己的自己。",
            "因为有了因为，所以才有所以，既然已成既然，何必再说何必。",
            "你就好似一轮太阳。有你我不一定能活，但没你我一定活不了。",
            "其实早听人说了，背叛是因为寂寞，只是我还没懂得，你把我当什么。",
            "世界上有很多种水果，但是，没有如果！",
            "一直以来你都是我的世界，我的世界丢失了。",
            "从来没有一种方式可以让我处之泰然，从来没有一种状态可以让我得以安宁。",
            "我不会再乞求，不为别人，为自己。",
            "我不仰望摩天轮，因为幸福离我太远。",
            "累了就睡觉，醒来就微笑。",
            "有些东西只能欣赏，不能品尝。",
            "曾为他相信明天就是未来。",
            "我宁愿只是争吵，还能道歉很好。",
            "你的过去，我来不及参与，你的未来我要紧紧相依。",
            "让暴风雨来的更猛烈点吧，让那些约会的都淋成落汤鸡。",
            "不要回头，已今晚了；你走向了天堂，我走向了地狱。",
            "我一直以为我是你的优乐美，可是你却坚决的选择了香飘飘。",
            "时间的沙漏沉淀着无法逃离的过往，记忆的双手总是拾起那些明媚的忧伤。",
            "纯真的笑容下，骨子里依然流着不安份的不明物体。",
            "过去的，没有人能把它们抓在手里，谁，都是活在此刻，活在无怨无悔的生命里。",
            "当生活给你设置重重关卡的时候，再撑一下，每次地咬牙闯关过后，你会发现想要的都在手中，想丢的都留在了身后。",
            "趁阳光正好。趁微风不噪。趁繁花还未开至荼蘼。趁现在还年轻，还可以走很长很长的路，还能诉说很深很深的思念。",
            "爱之于我，不是肌肤之亲，不是一蔬一饭。它是不死的欲望，是疲惫生活中的英雄梦想。",
            "有一颗独立的心，你才有一个完整的自我。有时，我们在舞台上不需要配角，也不需要观众，独自演义自己的角色，会表现得得淋漓尽致。",
            "沉默，不代表自己没话说。离开，不代表自己很潇洒。快乐，不代表自己没伤心。幸福，不代表自己没痛过。",
            "那个让你流泪的，是你最爱的人；那个懂你眼泪的，是最爱你的人。那个为你擦干眼泪的，才是最后和你相守的人。",
            "有时候，你原谅别人，只是因为你还想把他们留在你的生活里。",
            "有的人是真的不幸福，有的人只是无聊，只是不懂珍惜。",
            "学会一笑置之，超然待之，懂得隐忍，懂得原谅，让自己在宽容中壮大。",
            "为什么同是肉长得心，我痛得快死了，而TA可以如此逍遥，这么的决绝，是怎么做到的。",
            "谁先抱有期待谁就是输家，无情的人更易获得幸福。",
            "如果你觉得生命里的每扇门都被关上了，那请记住一句话：关上的门不一定上锁，至少再过去推一推。",
            "懂得越多，失去也越多，多少美好的事物也都经不住推敲。可是亲爱的，要知道，你可以看清、看透这个世界，却不要看破。",
            "活着就是一场寂寞与孤独的修行无论高尚与卑微，都要强烈追逐自己的命运，只有你想要，然后才可能拥有。",
            "很多人觉得自己活得太累，实际上他们可能只是睡得太晚。",
            "天空的飞鸟，是你的寂寞比我多，还是我的忧伤比你多，剩下的时光，你陪我，好不好，这样你不寂寞，我也不会忧伤……",
            "你也够俗的，这个谎话我前年就讲过了。",
            "蝴蝶，终究飞不过沧海。",
            "无论最后的俄们结果如何，我都愿意陪你到最后。",
            "成功的背后真的需要好多的辛酸。",
            "别点歌，别让伤心的情歌惹得你睡不安稳。",
            "原来真的有很多事，没开口就已经是从前。",
            "回忆总是会打我一巴掌，指着旧伤不准我遗忘。",
            "我拒绝了所有人的暧昧，只为等你的一个不确定的未来。",
            "有你在的一天必有我长伴左右。",
            "你坐在我心中最昂贵最赚眼泪的地方",
            "习惯了不能习惯的，那也就习惯成自然了。",
            "我一直在你的心门外进不去。",
            "有多少初三毕业的人，正在等着那要命的通知书。",
            "我若不勇敢，谁替我坚强。",
            "想念一个人有时也许会面带微笑，但你的心却会流泪。",
            "一朝春尽红颜老，花落人亡两不知。",
            "放手后的微笑，只是用来掩盖疼痛的伤疤。",
            "自从我们第一次吵架的时候，我就知道，我们不可能再维持下去。",
            "爱由心生，爱随心灭，记住爱情本来的模样，不为爱情迷失方向。",
            "原来一个人的孤单不算孤单，想念一个早已离去的人，才是真的孤单。",
            "我只是一个人走了太久，久到我已经习惯一个人了。",
            "每个人都是幸福的。只是，你的幸福，常常在别人眼里。",
            "那些人，那些事，过去的，再怎么修饰，都太苍白。",
            "一个微笑就能开出绚烂的爱情，一个眼神就能够抵抗寂寞。",
            "知足不代表不思进取，是对拥有的一切格外珍惜。",
            "孤单是你心里面没有人，寂寞是你心里有人却不在身边。",
            "如果有一天，我老无所依，请把我埋在，你的时光里。",
            "最怕空气忽然安静，最怕回忆突然翻滚绞痛着不平息。",
            "昨天永远是过去式，今天永远是进行式，明天永远是幻想式，天天都是方程式。",
            "如果可以，就算全世界就剩下我一个也无所谓。",
            "也不知道我未来的老公，现在正和谁恋爱。",
            "我们的爱情就像棉花糖，又甜又软，却总有一天会消失。",
            "即使输掉了一切也不要输掉微笑。",
            "回忆里的人我永远不想见，因为见了就没回忆了。",
            "命运决定谁会进入我们的生活，内心决定我们与谁并肩。",
            "爱情让青春蒙上了一层灰暗，原本清澈的眼眸隐藏着忧伤。",
            "你不知，在你转身错落的那个轮回间，我已万劫不复。"

    )
    val c = arrayOf(
        "I am not racist, but I only like FireBounce users. so git gut noobs",
        "FireBounce > ALL",
        "%p has a trash client",
        "%p have to get FireBounce to improve it's skills",
        "FireBounce > SilenceFix",
        "%p is not using FireBounce",
        "FireBounce was just born different",
        "FireBounce best the skid client",
        "What should I choose? FireBounce or FireBounce?",
        "FireBounce > Cheese",
        "We forgot to skid the s in skill",
        "why ur server is soooo lagggggg",
        "is it a bird? is it a plane? no its FireBounce!",
        "FireBounce users get MVP!",
        "%p eat some \$h1t",
        "i have a good FireBounce CONF1G, dont fuck off me",
        "I am not racist, but I only like FireBounce users. so git gut noobs",
        "What should I choose? FireBounce or FireBounce?",
        "im not hacking but using FireBounce",
        "FireBounce is rising!",
        "i like to eat pineapple pizza!",
        "FireBounce? HAX? skill!",
        "FireBounce > Tenacity",
        "sometimes i think bad things but they happens :(",
        "tbh just get Firebouunce lmao",
        "%p 主播你的技术烂成这样家里可以请哈基高了",
        "%p NM\$L",
        "%p u need to use IQBoost",
        "%p 已被FireBounce 击毙",
        "[XinXin提醒] >> 我们承诺: 不复当年有钱就赚保证就卖",
        "%p failed KILLAURA(A) vl=1337.0",
        "手感好+高敏而已",
        "%p 笨蛋..轻一点..唔..要..要去了..快..快停下♡",
        "%p 你这个技术去录视频收益绝对很高, 全价都四万了",
        "%p 你可以做我的显示器吗, 这样我就可以设你的比例了",
        "%p 可能需要食用一些 \$h¡t",
        "%p 啊～～轻一点啊～",
        "%p 请温柔一点～要受不了了～～呜呜",
        "%p ～～已经被哥哥～～玩的～要坏掉了",
        "%p 啊～～哥哥～轻点，要高抄了～啊～～哥哥的都流出来了",
        "%p 我想对你做，春天在樱桃树上的事",
        "%p 为什么把我去年瞎写丢垃圾桶的东西拿来圈",
        "%p 我不会让你哭的，除非在床上",
        "%p 使用Skyrim但是没有脑子的人是你吗",
        "%p 明明对哥哥的爱不掺水分，可是想你的时候，为什么总是湿湿的",
        "%p 咋了兄弟,要来偷刀?",
        "%p 你的操作马上就可以达到光速 因为没有任何质量",
        "%p 为什么抄木糖醇视觉",
        "%p 您需要之父90圆鸭金呢,因为老安卓4.5被泄露了呢",
        "%p 今天你崩端了吗 :)",
        "%p 主播你现在没有入权了?",
        "[XinXin提醒] >> 笑死我了这个欣欣无敌主播你那个NewNewVLC是有用的还是壳子"
    )
    val jokes = arrayOf(
        "%p 你知道为什么企鹅的肚子是白色的吗？因为它会害羞！",
        "如果 Minecraft 是现实，%p 早就因为重力摔死了",
        "%p 的挖矿技术比我的 AI 还原始",
        "%p 正在向母亲询问Ctrl键的位置 :>",
        "警告：检测到 %p 的智商过低，建议重启",
        "%p 的建筑风格让我想起了我的第一次尝试 - 灾难性的",
        "如果笨是一种资源，%p 将是服务器首富",
        "%p 的 PvP 技巧让我想起了我的奶奶 - 慢且可预测",
        "紧急通知：%p 刚刚尝试用木剑对抗下界合金套",
        "%p 的生存技能让我怀疑他们是如何活到现在的",
        "科学事实：观看 %p 玩游戏会导致脑细胞死亡",
        "%p 刚刚试图用钓鱼竿挖钻石 - 创新！",
        "如果 %p 的 Minecraft 技能是货币，他们会破产",
        "%p 的建筑让我想起了一个重要问题：为什么？",
        "警告：%p 正在尝试思考 - 这可能会很危险",
        "%p 的挖矿策略：希望钻石会自己跳出来",
        "如果 %p 的游戏技巧是超级英雄力量，他们会是'平庸侠'",
        "%p 刚刚试图用雪球对抗凋灵 - 勇敢！",
        "有趣的事实：%p 的死亡次数比这个服务器的区块还多",
        "%p 的农业技术让我想起了沙漠 - 贫瘠",
        "如果 %p 的红石知识是灯光，他们会生活在黑暗中",
        "%p 刚刚试图驯服苦力怕 - 自然选择在行动",
        "紧急警报：%p 正在尝试策略思考 - 系统过载",
        "%p 的探险技巧：走进每个明显的陷阱",
        "如果 %p 是 Minecraft 生物，他们会是迷路的羊",
        "%p 的建筑哲学：如果它立着，就是成功了",
        "为什么 %p 的熔炉从不抱怨？因为TA制作的的烹饪速度让它觉得自己在度假！",
        "%p 的红石电路太安全了，连骷髅都以为那是养老院走廊！",
        "听说 %p 挖矿时总带着水桶？岩浆见了你都喊'移动消防站'！",
        "%p 的建筑风格叫'抽象派安全屋'，苦力怕看了都选择绕道自爆！",
        "末影人偷 %p 方块的速度，还没你拆自己脚手架快！",
        "%p 的钓鱼技巧绝了，钓上来的靴子能开防水用品店！",
        "下界交通系统太环保，恶魂的火球都给你让道当路灯！",
        "附魔台看到 %p 的青金石说：'这次咱们试试碰运气模式'！",
        "%p 的凋灵召唤仪式，村民都搬来椅子看烟花秀！",
        "%p's 耕地技术超越时代，南瓜长得像像素迷宫！",
        "%p's 潜影盒里装的全是'我本来想放这里'的说明手册！",
        "末地传送门的光柱，是%p游戏生涯最笔直的建筑！",
        "%p's 三叉戟引雷成功率100%——专劈自己脚指头！",
        "%p's 村民交易站改叫慈善超市，绿宝石都是爱心捐赠！",
        "%p的下界合金升级法：岩浆池免费脱毛疗程！",
        "%p's地图画质设置里，云朵都选择自动避让模式！",
        "%p's刷怪塔效率惊人——每分钟产出1根骨头！",
        "%p's鞘翅飞行日志：撞山次数破世界纪录！",
        "%p's潮涌核心激活那刻，海底神庙集体申请失业救济！",
        "%p, 末影龙二阶段其实是你的拆床速度测试！"
    )
}