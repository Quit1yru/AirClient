/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.web

import fi.iki.elonen.NanoHTTPD
import net.ccbluex.liquidbounce.config.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleManager
import kotlin.math.floor

class HttpServer(port: Int) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession): Response {
        return when (session.uri) {
            "/" -> handleMainPage()
            "/modules" -> handleModulesPage(session)
            "/api/modules" -> handleModulesApi()
            "/api/module" -> handleModuleApi(session)
            "/api/module-state" -> handleModuleStateApi(session)
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "text/html", "404 Not Found")
        }
    }

    private fun handleMainPage(): Response {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>FireBounce Web UI</title>
                <meta charset="UTF-8">
                <style>
                    body {
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                        margin: 0;
                        padding: 0;
                        background-color: #0f0c29;
                        background-image: 
                            radial-gradient(circle at 10% 20%, rgba(15, 12, 41, 0.7) 0%, rgba(15, 12, 41, 0.7) 50%, transparent 50%),
                            radial-gradient(circle at 90% 80%, rgba(41, 12, 41, 0.7) 0%, rgba(41, 12, 41, 0.7) 50%, transparent 50%),
                            radial-gradient(circle at 50% 50%, rgba(25, 25, 60, 0.7) 0%, rgba(25, 25, 60, 0.7) 50%, transparent 50%);
                        color: #e0e0e0;
                        min-height: 100vh;
                        overflow-x: hidden;
                    }
                    #particles-js {
                        position: fixed;
                        width: 100%;
                        height: 100%;
                        top: 0;
                        left: 0;
                        z-index: -1;
                    }
                    header {
                        background: rgba(30, 30, 46, 0.85);
                        backdrop-filter: blur(10px);
                        padding: 20px;
                        box-shadow: 0 4px 20px rgba(0,0,0,0.5);
                        position: relative;
                        overflow: hidden;
                    }
                    header::before {
                        content: '';
                        position: absolute;
                        top: -50%;
                        left: -50%;
                        width: 200%;
                        height: 200%;
                        background: radial-gradient(circle, rgba(187, 134, 252, 0.1) 0%, transparent 70%);
                        animation: rotate 20s linear infinite;
                        z-index: -1;
                    }
                    @keyframes rotate {
                        from { transform: rotate(0deg); }
                        to { transform: rotate(360deg); }
                    }
                    h1 {
                        color: #bb86fc;
                        margin: 0;
                        text-align: center;
                        font-size: 2.5rem;
                        text-shadow: 0 0 10px rgba(187, 134, 252, 0.5);
                        position: relative;
                        z-index: 1;
                    }
                    .container {
                        max-width: 1200px;
                        margin: 20px auto;
                        padding: 0 20px;
                    }
                    .category-grid {
                        display: grid;
                        grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
                        gap: 20px;
                        margin-top: 25px;
                    }
                    .category-card {
                        background: rgba(30, 30, 46, 0.8);
                        backdrop-filter: blur(10px);
                        border-radius: 12px;
                        padding: 20px;
                        text-align: center;
                        cursor: pointer;
                        transition: all 0.3s ease;
                        box-shadow: 0 8px 16px rgba(0,0,0,0.3);
                        border: 1px solid rgba(187, 134, 252, 0.2);
                        position: relative;
                        overflow: hidden;
                        min-height: 120px;
                        display: flex;
                        flex-direction: column;
                        justify-content: center;
                    }
                    .category-card::before {
                        content: '';
                        position: absolute;
                        top: 0;
                        left: 0;
                        width: 100%;
                        height: 100%;
                        background: linear-gradient(45deg, transparent, rgba(187, 134, 252, 0.1), transparent);
                        transform: translateX(-100%);
                        transition: transform 0.6s ease;
                    }
                    .category-card:hover::before {
                        transform: translateX(100%);
                    }
                    .category-card:hover {
                        transform: translateY(-8px) scale(1.02);
                        box-shadow: 0 12px 25px rgba(187, 134, 252, 0.3);
                        border-color: rgba(187, 134, 252, 0.5);
                    }
                    .category-icon {
                        font-size: 48px;
                        margin-bottom: 15px;
                        background: linear-gradient(45deg, #bb86fc, #03dac6);
                        -webkit-background-clip: text;
                        -webkit-text-fill-color: transparent;
                        text-shadow: 0 0 15px rgba(187, 134, 252, 0.3);
                    }
                    .category-name {
                        font-size: 18px;
                        font-weight: bold;
                        color: #bb86fc;
                        margin-bottom: 8px;
                    }
                    .module-count {
                        font-size: 14px;
                        color: #9e9e9e;
                    }
                    .search-container {
                        margin: 25px 0;
                        text-align: center;
                        position: relative;
                    }
                    #searchInput {
                        width: 100%;
                        max-width: 600px;
                        padding: 12px 20px;
                        border-radius: 30px;
                        border: 2px solid rgba(187, 134, 252, 0.3);
                        background: rgba(30, 30, 46, 0.7);
                        color: #fff;
                        font-size: 16px;
                        outline: none;
                        transition: all 0.3s ease;
                        backdrop-filter: blur(5px);
                    }
                    #searchInput:focus {
                        border-color: #bb86fc;
                        box-shadow: 0 0 20px rgba(187, 134, 252, 0.5);
                    }
                    .back-link {
                        display: inline-block;
                        margin: 20px 0;
                        color: #bb86fc;
                        text-decoration: none;
                        font-weight: bold;
                        font-size: 16px;
                        padding: 8px 16px;
                        border-radius: 30px;
                        background: linear-gradient(45deg, #6200ee, #3700b3);
                        color: white;
                        transition: all 0.3s ease;
                        backdrop-filter: blur(5px);
                        box-shadow: 0 0 10px rgba(98, 0, 238, 0.5);
                    }
                    .back-link:hover {
                        background: linear-gradient(45deg, #bb86fc, #3700b3);
                        text-shadow: 0 0 10px rgba(187, 134, 252, 0.7);
                        transform: translateX(-5px);
                        box-shadow: 0 0 20px rgba(187, 134, 252, 0.8);
                    }
                    .module-controls {
                        display: flex;
                        align-items: center;
                        gap: 8px;
                    }
                    .toggle-arrow {
                        display: inline-block;
                        font-size: 18px;
                        cursor: pointer;
                        transition: transform 0.3s ease;
                        color: #bb86fc;
                        width: 18px;
                        text-align: center;
                    }
                    .toggle-arrow.rotated {
                        transform: rotate(90deg);
                    }
                </style>
            </head>
            <body>
                <div id="particles-js"></div>
                <header>
                    <h1>FireBounce Web UI</h1>
                </header>
                
                <div class="container">
                    <div class="search-container">
                        <input type="text" id="searchInput" placeholder="搜索模块..." onkeydown="if(event.key==='Enter') searchModules()">
                    </div>
                    
                    <div class="category-grid">
                        ${
            Category.entries.joinToString("") { category ->
                val moduleCount = ModuleManager[category].size
                """
                            <div class="category-card" onclick="window.location.href='/modules?category=${category.name}'">
                                <div class="category-icon">📁</div>
                                <div class="category-name">${category.displayName}</div>
                                <div class="module-count">$moduleCount 个模块</div>
                            </div>
                            """
            }}
                    </div>
                </div>
                
                <script>
                    // 粒子背景效果
                    function initParticles() {
                        const canvas = document.createElement('canvas');
                        canvas.id = 'particles-canvas';
                        canvas.style.position = 'absolute';
                        canvas.style.top = '0';
                        canvas.style.left = '0';
                        canvas.style.width = '100%';
                        canvas.style.height = '100%';
                        canvas.style.zIndex = '-1';
                        document.getElementById('particles-js').appendChild(canvas);
                        
                        const ctx = canvas.getContext('2d');
                        let particles = [];
                        const particleCount = 100;
                        
                        function resizeCanvas() {
                            canvas.width = window.innerWidth;
                            canvas.height = window.innerHeight;
                        }
                        
                        function createParticles() {
                            particles = [];
                            for (let i = 0; i < particleCount; i++) {
                                particles.push({
                                    x: Math.random() * canvas.width,
                                    y: Math.random() * canvas.height,
                                    radius: Math.random() * 2 + 1,
                                    vx: (Math.random() - 0.5) * 0.5,
                                    vy: (Math.random() - 0.5) * 0.5,
                                    color: `rgba(${floor(Math.random() * 100 + 155)}, ${floor(Math.random() * 100 + 100)}, 255, ${Math.random() * 0.5 + 0.2})`
                                });
                            }
                        }
                        
                        function drawParticles() {
                            ctx.clearRect(0, 0, canvas.width, canvas.height);
                            
                            for (let i = 0; i < particles.length; i++) {
                                const p = particles[i];
                                
                                ctx.beginPath();
                                ctx.arc(p.x, p.y, p.radius, 0, Math.PI * 2);
                                ctx.fillStyle = p.color;
                                ctx.fill();
                                
                                // 更新粒子位置
                                p.x += p.vx;
                                p.y += p.vy;
                                
                                // 边界检测
                                if (p.x < 0 || p.x > canvas.width) p.vx *= -1;
                                if (p.y < 0 || p.y > canvas.height) p.vy *= -1;
                            }
                            
                            requestAnimationFrame(drawParticles);
                        }
                        
                        window.addEventListener('resize', () => {
                            resizeCanvas();
                            createParticles();
                        });
                        
                        resizeCanvas();
                        createParticles();
                        drawParticles();
                    }
                    
                    // 初始化粒子效果
                    window.addEventListener('load', initParticles);
                    
                    function searchModules() {
                        const input = document.getElementById('searchInput');
                        const filter = input.value.toLowerCase();
                        if (filter.length > 0) {
                            window.location.href = '/modules?search=' + encodeURIComponent(filter);
                        } else {
                            window.location.href = '/modules';
                        }
                    }
                    
                    function toggleModuleValues(moduleName) {
                        const valuesDiv = document.getElementById('values-' + moduleName);
                        const arrow = event.target;
                        
                        if (valuesDiv.style.display === 'none') {
                            valuesDiv.style.display = 'block';
                            arrow.classList.add('rotated');
                        } else {
                            valuesDiv.style.display = 'none';
                            arrow.classList.remove('rotated');
                        }
                    }
                </script>
            </body>
            </html>
        """.trimIndent()

        return newFixedLengthResponse(html)
    }

    private fun handleModulesPage(session: IHTTPSession): Response {
        val modulesHtml = buildString {
            append("""
                <!DOCTYPE html>
                <html>
                <head>
                    <title>模块管理 - FireBounce Web UI</title>
                    <meta charset="UTF-8">
                    <style>
                        body {
                            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                            margin: 0;
                            padding: 0;
                            background-color: #0f0c29;
                            background-image: 
                                radial-gradient(circle at 10% 20%, rgba(15, 12, 41, 0.7) 0%, rgba(15, 12, 41, 0.7) 50%, transparent 50%),
                                radial-gradient(circle at 90% 80%, rgba(41, 12, 41, 0.7) 0%, rgba(41, 12, 41, 0.7) 50%, transparent 50%),
                                radial-gradient(circle at 50% 50%, rgba(25, 25, 60, 0.7) 0%, rgba(25, 25, 60, 0.7) 50%, transparent 50%);
                            color: #e0e0e0;
                            min-height: 100vh;
                        }
                        #particles-js {
                            position: fixed;
                            width: 100%;
                            height: 100%;
                            top: 0;
                            left: 0;
                            z-index: -1;
                        }
                        header {
                            background: rgba(30, 30, 46, 0.85);
                            backdrop-filter: blur(10px);
                            padding: 20px;
                            box-shadow: 0 4px 20px rgba(0,0,0,0.5);
                            position: relative;
                        }
                        h1 {
                            color: #bb86fc;
                            margin: 0;
                            text-align: center;
                            font-size: 2.2rem;
                            text-shadow: 0 0 10px rgba(187, 134, 252, 0.5);
                        }
                        .container {
                            max-width: 1200px;
                            margin: 20px auto;
                            padding: 0 20px;
                        }
                        .back-link {
                            display: inline-block;
                            margin: 20px 0;
                            color: #bb86fc;
                            text-decoration: none;
                            font-weight: bold;
                            font-size: 16px;
                            padding: 8px 16px;
                            border-radius: 30px;
                            background: linear-gradient(45deg, #6200ee, #3700b3);
                            color: white;
                            transition: all 0.3s ease;
                            backdrop-filter: blur(5px);
                            box-shadow: 0 0 10px rgba(98, 0, 238, 0.5);
                        }
                        .back-link:hover {
                            background: linear-gradient(45deg, #bb86fc, #3700b3);
                            text-shadow: 0 0 10px rgba(187, 134, 252, 0.7);
                            transform: translateX(-5px);
                            box-shadow: 0 0 20px rgba(187, 134, 252, 0.8);
                        }
                        .search-container {
                            margin: 25px 0;
                            text-align: center;
                        }
                        #searchInput {
                            width: 100%;
                            max-width: 600px;
                            padding: 12px 20px;
                            border-radius: 30px;
                            border: 2px solid rgba(187, 134, 252, 0.3);
                            background: rgba(30, 30, 46, 0.7);
                            color: #fff;
                            font-size: 16px;
                            outline: none;
                            transition: all 0.3s ease;
                            backdrop-filter: blur(5px);
                        }
                        #searchInput:focus {
                            border-color: #bb86fc;
                            box-shadow: 0 0 20px rgba(187, 134, 252, 0.5);
                        }
                        .category-header {
                            background: rgba(30, 30, 46, 0.7);
                            padding: 18px;
                            border-radius: 12px;
                            margin: 20px 0;
                            text-align: center;
                            box-shadow: 0 8px 16px rgba(0,0,0,0.3);
                            border: 1px solid rgba(187, 134, 252, 0.2);
                            backdrop-filter: blur(10px);
                        }
                        .category-name {
                            font-size: 24px;
                            color: #bb86fc;
                            margin: 0;
                            text-shadow: 0 0 10px rgba(187, 134, 252, 0.5);
                        }
                        .module-grid {
                            display: grid;
                            grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
                            gap: 20px;
                            align-items: start;
                        }
                        .module-card {
                            background: rgba(30, 30, 46, 0.7);
                            border-radius: 12px;
                            padding: 20px;
                            box-shadow: 0 8px 16px rgba(0,0,0,0.3);
                            transition: all 0.3s ease;
                            border: 1px solid rgba(187, 134, 252, 0.2);
                            backdrop-filter: blur(10px);
                            display: flex;
                            flex-direction: column;
                        }
                        .module-card:hover {
                            transform: translateY(-5px);
                            box-shadow: 0 12px 25px rgba(187, 134, 252, 0.3);
                            border-color: rgba(187, 134, 252, 0.5);
                        }
                        .module-header {
                            display: flex;
                            justify-content: space-between;
                            align-items: center;
                            margin-bottom: 15px;
                            padding-bottom: 12px;
                            border-bottom: 1px solid rgba(187, 134, 252, 0.3);
                        }
                        .module-name {
                            font-weight: bold;
                            font-size: 18px;
                            color: #bb86fc;
                        }
                        .module-toggle {
                            padding: 6px 12px;
                            border: 2px solid #ffffff;
                            border-radius: 30px;
                            cursor: pointer;
                            font-weight: bold;
                            transition: all 0.3s ease;
                            font-size: 13px;
                            background: transparent;
                            color: #ffffff;
                            min-width: 60px;
                        }
                        .module-toggle.on {
                            background: rgba(3, 218, 198, 0.2);
                            color: #03dac6;
                            box-shadow: 0 0 10px rgba(3, 218, 198, 0.5);
                        }
                        .module-toggle.off {
                            background: transparent;
                            color: #ffffff;
                        }
                        .module-toggle:hover {
                            transform: scale(1.05);
                            box-shadow: 0 0 15px rgba(255, 255, 255, 0.5);
                        }
                        .module-values {
                            margin-top: 15px;
                            display: none;
                        }
                        .value {
                            margin: 10px 0;
                            padding: 10px;
                            background: rgba(25, 25, 40, 0.5);
                            border-radius: 8px;
                        }
                        .value label {
                            display: block;
                            margin-bottom: 6px;
                            font-weight: bold;
                            color: #ccc;
                            font-size: 14px;
                        }
                        input[type="checkbox"] {
                            width: 18px;
                            height: 18px;
                        }
                        input[type="number"], input[type="text"], select {
                            width: 100%;
                            padding: 10px;
                            border: 1px solid rgba(187, 134, 252, 0.3);
                            border-radius: 8px;
                            background: rgba(20, 20, 35, 0.7);
                            color: #fff;
                            font-size: 14px;
                            transition: all 0.3s ease;
                        }
                        input[type="number"]:focus, input[type="text"]:focus, select:focus {
                            border-color: #bb86fc;
                            box-shadow: 0 0 15px rgba(187, 134, 252, 0.5);
                            outline: none;
                        }
                        .range-container {
                            display: flex;
                            align-items: center;
                            gap: 5px;
                        }
                        .range-container input {
                            flex: 1;
                        }
                        .range-separator {
                            padding: 0 5px;
                        }
                        .module-controls {
                            display: flex;
                            align-items: center;
                            gap: 8px;
                        }
                        .toggle-arrow {
                            display: inline-block;
                            font-size: 16px;
                            cursor: pointer;
                            transition: transform 0.3s ease;
                            color: #bb86fc;
                            width: 16px;
                            text-align: center;
                        }
                        .toggle-arrow.rotated {
                            transform: rotate(90deg);
                        }
                        .no-modules {
                            text-align: center;
                            padding: 50px;
                            color: #9e9e9e;
                            font-style: italic;
                            font-size: 18px;
                            background: rgba(30, 30, 46, 0.5);
                            border-radius: 12px;
                            backdrop-filter: blur(10px);
                        }
                    </style>
                </head>
                <body>
                    <div id="particles-js"></div>
                    <header>
                        <h1>模块管理</h1>
                    </header>
                    
                    <div class="container">
                        <a href="/" class="back-link">&larr; 返回主页</a>
                        
                        <div class="search-container">
                            <input type="text" id="searchInput" placeholder="搜索模块..." onkeydown="if(event.key==='Enter') searchModules()">
                        </div>
            """.trimIndent())

            // 获取查询参数
            val queryParams = session.parms
            val searchQuery = queryParams["search"] ?: ""
            val categoryParam = queryParams["category"]

            if (searchQuery.isNotEmpty()) {
                // 搜索模式
                val filteredModules = ModuleManager.filter { module ->
                    module.name.contains(searchQuery, ignoreCase = true) ||
                            module.description.contains(searchQuery, ignoreCase = true)
                }

                if (filteredModules.isEmpty()) {
                    append("""<div class="no-modules">未找到匹配的模块</div>""")
                } else {
                    append("""<div class="module-grid">""")
                    for (module in filteredModules.sortedBy { it.name }) {
                        appendModuleCard(module)
                    }
                    append("""</div>""")
                }
            } else if (categoryParam != null) {
                // 分类模式
                val category = Category.entries.find { it.name.equals(categoryParam, ignoreCase = true) }
                if (category != null) {
                    append("""
                        <div class="category-header">
                            <h2 class="category-name">${category.displayName}</h2>
                        </div>
                    """.trimIndent())

                    val modulesInCategory = ModuleManager[category]
                    if (modulesInCategory.isEmpty()) {
                        append("""<div class="no-modules">此类别下没有模块</div>""")
                    } else {
                        append("""<div class="module-grid">""")
                        for (module in modulesInCategory.sortedBy { it.name }) {
                            appendModuleCard(module)
                        }
                        append("""</div>""")
                    }
                } else {
                    append("""<div class="no-modules">无效的类别</div>""")
                }
            } else {
                // 显示所有模块
                append("""<div class="module-grid">""")
                for (module in ModuleManager.sortedBy { it.name }) {
                    appendModuleCard(module)
                }
                append("""</div>""")
            }

            append("""
                        <script>
                            // 粒子背景效果
                            function initParticles() {
                                const canvas = document.createElement('canvas');
                                canvas.id = 'particles-canvas';
                                canvas.style.position = 'absolute';
                                canvas.style.top = '0';
                                canvas.style.left = '0';
                                canvas.style.width = '100%';
                                canvas.style.height = '100%';
                                canvas.style.zIndex = '-1';
                                document.getElementById('particles-js').appendChild(canvas);
                                
                                const ctx = canvas.getContext('2d');
                                let particles = [];
                                const particleCount = 80;
                                
                                function resizeCanvas() {
                                    canvas.width = window.innerWidth;
                                    canvas.height = window.innerHeight;
                                }
                                
                                function createParticles() {
                                    particles = [];
                                    for (let i = 0; i < particleCount; i++) {
                                        particles.push({
                                            x: Math.random() * canvas.width,
                                            y: Math.random() * canvas.height,
                                            radius: Math.random() * 2 + 1,
                                            vx: (Math.random() - 0.5) * 0.5,
                                            vy: (Math.random() - 0.5) * 0.5,
                                            color: `rgba(${floor(Math.random() * 100 + 155)}, ${floor(Math.random() * 100 + 100)}, 255, ${Math.random() * 0.5 + 0.2})`
                                        });
                                    }
                                }
                                
                                function drawParticles() {
                                    ctx.clearRect(0, 0, canvas.width, canvas.height);
                                    
                                    for (let i = 0; i < particles.length; i++) {
                                        const p = particles[i];
                                        
                                        ctx.beginPath();
                                        ctx.arc(p.x, p.y, p.radius, 0, Math.PI * 2);
                                        ctx.fillStyle = p.color;
                                        ctx.fill();
                                        
                                        // 更新粒子位置
                                        p.x += p.vx;
                                        p.y += p.vy;
                                        
                                        // 边界检测
                                        if (p.x < 0 || p.x > canvas.width) p.vx *= -1;
                                        if (p.y < 0 || p.y > canvas.height) p.vy *= -1;
                                    }
                                    
                                    requestAnimationFrame(drawParticles);
                                }
                                
                                window.addEventListener('resize', () => {
                                    resizeCanvas();
                                    createParticles();
                                });
                                
                                resizeCanvas();
                                createParticles();
                                drawParticles();
                            }
                            
                            // 初始化粒子效果
                            window.addEventListener('load', initParticles);
                            
                            function toggleModule(moduleName) {
                                fetch('/api/module?name=' + encodeURIComponent(moduleName), {
                                    method: 'POST'
                                }).then(() => {
                                    location.reload();
                                });
                            }
                            
                            function updateValue(moduleName, valueName, value) {
                                fetch('/api/module?name=' + encodeURIComponent(moduleName) + '&valuename=' + encodeURIComponent(valueName) + '&value=' + encodeURIComponent(value), {
                                    method: 'PUT'
                                }).then(response => {
                                    if (!response.ok) {
                                        alert('更新失败: ' + response.status);
                                    } else {
                                        // 成功更新后检查依赖关系，实时更新UI
                                        refreshValueDependencies(moduleName);
                                    }
                                });
                            }
                            
                            function updateIntRangeValue(moduleName, valueName) {
                                const minInput = document.getElementById(moduleName + '_' + valueName + '_min');
                                const maxInput = document.getElementById(moduleName + '_' + valueName + '_max');
                                const minValue = parseInt(minInput.value) || 0;
                                const maxValue = parseInt(maxInput.value) || 0;
                                const rangeValue = minValue + '..' + maxValue;
                                updateValue(moduleName, valueName, rangeValue);
                            }
                            
                            function updateFloatRangeValue(moduleName, valueName) {
                                const minInput = document.getElementById(moduleName + '_' + valueName + '_min');
                                const maxInput = document.getElementById(moduleName + '_' + valueName + '_max');
                                const minValue = parseFloat(minInput.value) || 0.0;
                                const maxValue = parseFloat(maxInput.value) || 0.0;
                                const rangeValue = minValue + '..' + maxValue;
                                updateValue(moduleName, valueName, rangeValue);
                            }
                            
                            function refreshValueDependencies(moduleName) {
                                // 获取模块的最新状态
                                fetch('/api/module-state?name=' + encodeURIComponent(moduleName))
                                    .then(response => response.text())
                                    .then(text => {
                                        try {
                                            const data = JSON.parse(text);
                                            // 更新每个值的可见性
                                            if (data && data.values) {
                                                data.values.forEach(val => {
                                                    const valueElement = document.getElementById('value-' + moduleName + '-' + val.name);
                                                    if (valueElement) {
                                                        valueElement.style.display = val.shouldRender ? 'block' : 'none';
                                                    }
                                                });
                                            }
                                        } catch (e) {
                                            console.error('Error parsing JSON:', e);
                                        }
                                    })
                                    .catch(error => {
                                        console.error('Error refreshing dependencies:', error);
                                    });
                            }
                            
                            function searchModules() {
                                const input = document.getElementById('searchInput');
                                const filter = input.value.toLowerCase();
                                if (filter.length > 0) {
                                    window.location.href = '/modules?search=' + encodeURIComponent(filter);
                                } else {
                                    window.location.href = '/modules';
                                }
                            }
                            
                            function toggleModuleValues(moduleName) {
                                const valuesDiv = document.getElementById('values-' + moduleName);
                                const arrow = event.target;
                                
                                if (valuesDiv.style.display === 'none') {
                                    valuesDiv.style.display = 'block';
                                    arrow.classList.add('rotated');
                                } else {
                                    valuesDiv.style.display = 'none';
                                    arrow.classList.remove('rotated');
                                }
                            }
                        </script>
                    </body>
                </html>
            """.trimIndent())
        }

        return newFixedLengthResponse(modulesHtml)
    }

    private fun StringBuilder.appendModuleCard(module: Module) {
        val stateClass = if (module.state) "on" else "off"
        val stateText = if (module.state) "开启" else "关闭"

        append("""
            <div class="module-card">
                <div class="module-header">
                    <span class="module-name">${module.name}</span>
                    <div class="module-controls">
                        ${if (module.values.isNotEmpty()) """<span class="toggle-arrow" onclick="toggleModuleValues('${module.name}')">></span>""" else ""}
                        <button class="module-toggle $stateClass" onclick="toggleModule('${module.name}')">$stateText</button>
                    </div>
                </div>
        """.trimIndent())

        if (module.values.isNotEmpty()) {
            append("""<div class="module-values" id="values-${module.name}" style="display: none;">""")
            // 只显示应该渲染的值（根据shouldRender()方法）
            for (value in module.values.filter { !it.hidden && !it.excluded }) {
                // 使用内联样式根据shouldRender()决定是否显示
                val displayStyle = if (value.shouldRender()) "block" else "none"
                append("""<div class="value" id="value-${module.name}-${value.name}" style="display: $displayStyle;">""")
                append("""<label>${value.name}:</label>""")

                when (value) {
                    is BoolValue -> {
                        val checked = if (value.get()) "checked" else ""
                        append("""<input type="checkbox" id="${module.name}_${value.name}" $checked onchange="updateValue('${module.name}', '${value.name}', this.checked ? 'true' : 'false')">""")
                    }
                    is IntValue -> {
                        append("""<input type="number" id="${module.name}_${value.name}" value="${value.get()}" onchange="updateValue('${module.name}', '${value.name}', this.value)">""")
                    }
                    is FloatValue -> {
                        append("""<input type="number" step="0.1" id="${module.name}_${value.name}" value="${value.get()}" onchange="updateValue('${module.name}', '${value.name}', this.value)">""")
                    }
                    is TextValue -> {
                        append("""<input type="text" id="${module.name}_${value.name}" value="${value.get()}" onchange="updateValue('${module.name}', '${value.name}', this.value)">""")
                    }
                    is ListValue -> {
                        append("""<select id="${module.name}_${value.name}" onchange="updateValue('${module.name}', '${value.name}', this.value)">""")
                        for (choice in value.values) {
                            val selected = if (choice == value.get()) "selected" else ""
                            append("""<option value="$choice" $selected>$choice</option>""")
                        }
                        append("""</select>""")
                    }
                    is IntRangeValue -> {
                        val range = value.get()
                        append("""
                            <div class="range-container">
                                <input type="number" id="${module.name}_${value.name}_min" value="${range.first}" onchange="updateIntRangeValue('${module.name}', '${value.name}')">
                                <span class="range-separator"> - </span>
                                <input type="number" id="${module.name}_${value.name}_max" value="${range.last}" onchange="updateIntRangeValue('${module.name}', '${value.name}')">
                            </div>
                        """.trimIndent())
                    }
                    is FloatRangeValue -> {
                        val range = value.get()
                        append("""
                            <div class="range-container">
                                <input type="number" step="0.1" id="${module.name}_${value.name}_min" value="${range.start}" onchange="updateFloatRangeValue('${module.name}', '${value.name}')">
                                <span class="range-separator"> - </span>
                                <input type="number" step="0.1" id="${module.name}_${value.name}_max" value="${range.endInclusive}" onchange="updateFloatRangeValue('${module.name}', '${value.name}')">
                            </div>
                        """.trimIndent())
                    }
                    else -> {
                        // For other value types, show as text input
                        append("""<input type="text" id="${module.name}_${value.name}" value="${value.get()}" onchange="updateValue('${module.name}', '${value.name}', this.value)">""")
                    }
                }
                append("</div>")
            }
            append("</div>")
        }

        append("</div>")
    }

    private fun handleModulesApi(): Response {
        val modules = ModuleManager.map { module ->
            mapOf(
                "name" to module.name,
                "state" to module.state,
                "values" to module.values.filter { !it.hidden && !it.excluded }.map { value ->
                    mapOf(
                        "name" to value.name,
                        "value" to value.get().toString(),
                        "type" to value::class.java.simpleName
                    )
                }
            )
        }

        return newFixedLengthResponse(Response.Status.OK, "application/json", modules.toString())
    }

    private fun handleModuleStateApi(session: IHTTPSession): Response {
        val params = session.parms
        val moduleName = params["name"] ?: return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", """{"error": "Missing module name"}""")
        val module = ModuleManager[moduleName] ?: return newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json", """{"error": "Module not found"}""")

        // 手动构建JSON响应
        val valuesJson = module.values.filter { !it.hidden && !it.excluded }.joinToString(",") { value ->
            """{"name":"${value.name}","value":"${value.get().toString().replace("\"", "\\\"")}","shouldRender":${value.shouldRender()}}"""
        }

        val jsonResponse = """{"name":"${module.name}","state":${module.state},"values":[${valuesJson}]}"""

        return newFixedLengthResponse(Response.Status.OK, "application/json", jsonResponse)
    }

    private fun handleModuleApi(session: IHTTPSession): Response {
        val params = session.parms
        val moduleName = params["name"] ?: return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Missing module name")
        val module = ModuleManager[moduleName] ?: return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Module not found")

        return when (session.method) {
            Method.POST -> {
                // Toggle module state
                module.toggle()
                newFixedLengthResponse(Response.Status.OK, "text/plain", "Module toggled")
            }
            Method.PUT -> {
                // Update module value
                val valueName = params["valuename"] ?: return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Missing value name")
                val valueStr = params["value"] ?: return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Missing value")

                val value = module.getValue(valueName)
                    ?: return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Value not found")

                try {
                    when (value) {
                        is BoolValue -> value.set(valueStr.toBoolean())
                        is IntValue -> value.set(valueStr.toInt())
                        is FloatValue -> value.set(valueStr.toFloat())
                        is TextValue -> value.set(valueStr)
                        is ListValue -> value.set(valueStr)
                        is IntRangeValue -> {
                            // 解析 "min..max" 格式的范围值
                            if (valueStr.contains("..")) {
                                val parts = valueStr.split("..")
                                if (parts.size == 2) {
                                    val min = parts[0].toIntOrNull() ?: 0
                                    val max = parts[1].toIntOrNull() ?: 0
                                    value.set(min..max)
                                }
                            }
                        }
                        is FloatRangeValue -> {
                            // 解析 "min..max" 格式的范围值
                            if (valueStr.contains("..")) {
                                val parts = valueStr.split("..")
                                if (parts.size == 2) {
                                    val min = parts[0].toFloatOrNull() ?: 0f
                                    val max = parts[1].toFloatOrNull() ?: 0f
                                    value.set(min..max)
                                }
                            }
                        }
                        else -> {
                            // For other value types, try to set as string using reflection
                            try {
                                value.javaClass.getMethod("set", String::class.java).invoke(value, valueStr)
                            } catch (_: Exception) {
                                // 如果反射调用失败，就忽略错误
                            }
                        }
                    }
                    newFixedLengthResponse(Response.Status.OK, "text/plain", "Value updated")
                } catch (e: Exception) {
                    newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Invalid value: ${e.message}")
                }
            }
            else -> newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, "text/plain", "Method not allowed")
        }
    }
}