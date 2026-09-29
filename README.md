# StockVision 股票行情查看 App (专注 A 股与全球市场)

一款现代化的股票行情查看 Android 客户端应用与 Cloudflare Worker 边缘计算服务端，**专为 A 股投资者优化**。

### 🚀 实时在线服务与发布下载
- **📦 GitHub Release 正式发布**: [**Release v1.0.4 页面**](https://github.com/lublue147-netizen/stock-trading-system/releases/tag/v1.0.4)
- **🌟 正式 Release APK 下载**: [**`StockVision-Release.apk`**](https://github.com/lublue147-netizen/stock-trading-system/releases/download/v1.0.4/StockVision-Release.apk) *(已锁定永久签名，支持直接覆盖升级安装)*
- **🛠️ Debug APK 下载**: [**`StockVision-Debug.apk`**](https://github.com/lublue147-netizen/stock-trading-system/releases/download/v1.0.4/StockVision-Debug.apk)
- **GitHub 仓库**: [lublue147-netizen/stock-trading-system](https://github.com/lublue147-netizen/stock-trading-system)
- **已部署 Cloudflare Worker**: [`https://stock-trading-worker.lublue147.workers.dev`](https://stock-trading-worker.lublue147.workers.dev)
  - 健康检查: [`/api/health`](https://stock-trading-worker.lublue147.workers.dev/api/health)
  - 茅台行情 (含盘口全指标): [`/api/quote?symbol=600519`](https://stock-trading-worker.lublue147.workers.dev/api/quote?symbol=600519)
  - 比亚迪行情: [`/api/quote?symbol=002594`](https://stock-trading-worker.lublue147.workers.dev/api/quote?symbol=002594)
  - A 股大盘指数: [`/api/market/indices`](https://stock-trading-worker.lublue147.workers.dev/api/market/indices)
  - 拼音/汉字搜索: [`/api/search?q=BYD`](https://stock-trading-worker.lublue147.workers.dev/api/search?q=BYD)
  - 历史行情: [`/api/history?symbol=600519&range=1mo`](https://stock-trading-worker.lublue147.workers.dev/api/history?symbol=600519&range=1mo)

### 📊 东方财富 (East Money) 风格个股深度行情全面升级 (v1.0.4)
1. **顶部状态栏**: 股票名称、沪/深/北/港交易所色彩角标、纯6位股票代码、实时交易状态指示器（交易中、已收盘、盘前竞价）。
2. **巨幅价格区与涨跌停**: 超大醒目现价显示，涨跌额与百分比，精确计算展示涨停价与跌停价。
3. **东方财富 12 宫格核心盘口**:
   - 今开、昨收、最高、最低、涨停、跌停
   - 成交量 (手 / 万手)、成交额 (亿 / 万)、换手率 %、振幅 %
   - 市盈率 (动 / TTM)、市净率、总市值、流通值、每股收益、委比
4. **经典 6 大周期切换**: 【分时】、【五日】、【日K】、【周K】、【月K】、【全部】。
5. **分时与 K 线双模式交互 Canvas**:
   - 分时图绘制昨收参考基准虚线，平滑走势曲线与透明渐变填充；
   - K 线图动态叠加 MA5 (黄)、MA10 (紫)、MA20 (青) 均线；
   - 副图绘制红绿成交量柱与 MAVOL5 / MAVOL10 均量线；
   - 手势触摸与拖拽十字交叉光标 (Crosshair)，精准探查时间、开高低收、量额与涨跌幅。
6. **买卖五档盘口 (Five-Level Order Book)**:
   - 东方财富经典盘口布局：卖五 ~ 卖一、买一 ~ 买五实时挂单价与手量；
   - 包含水平深度条可视化进度；
   - 中间栏直观展示委比 (%) 与委差 (手)。
7. **F10 资料与个股简况**: 所属行业分类、主营业务详细描述、概念题材板块标签流、每股收益 (EPS)、每股净资产 (BPS)、净资产收益率 (ROE)。
8. **底部悬浮快捷操作栏**: 加自选 / 已在自选切换、一键设行情预警、即时刷新。

### 🇨🇳 A 股核心深度适配
1. **国内五大指数实时直击**: 上证指数、深证成指、创业板指、科创50、沪深300。
2. **纯6位股票代码自动识别**: 无论输入 `600519`、`300750`、`002594` 均可自动补齐后缀并即时拉取。
3. **拼音缩写与汉字联想搜索**: 支持输入 `BYD`、`GZMT`、`NDSD`、`DFCF` 或 `茅台`、`宁德` 模糊匹配。
4. **纯正 A 股红涨绿跌习惯**: 默认启用红色代表上涨、绿色代表下跌的国内交易配色方案。
5. **A 股龙头默认自选池**: 预置贵州茅台、宁德时代、比亚迪、中国平安、招商银行、东方财富、平安银行、五粮液、中芯国际等标的。

---

## 🌟 核心特性

- 📱 **纯血现代 Android 原生架构**: 基于 **Kotlin + Jetpack Compose + Material 3 + 协程 (Coroutines/StateFlow)** 构建，界面流畅丝滑。
- 📊 **专业交互式历史行情图表 (Canvas 原生绘制)**:
  - 支持 **K线图 (蜡烛图)** 与 **分时走势图 (折线平滑填充)** 自由切换；
  - 支持 **多周期切换**: `1D` (分时), `5D` (5日), `1M` (月K), `6M` (半年), `1Y` (年K), `ALL` (全部历史)；
  - 动态计算并叠加 **MA5 / MA10 / MA20** 移动均线指标（可一键显示/隐藏）；
  - 底部同步绘制 **成交量柱状图 (Volume)**；
  - 支持 **手势触摸十字交叉线 (Crosshair)** 与长按滑动探测，实时浮动展示选中时间点的 开盘价、最高价、最低价、收盘价、成交量及涨跌幅。
- ⭐️ **自选股票管理 (Watchlist)**:
  - 一键添加/移除自选股，本地持久化保存；
  - 自选列表支持股票实时价格、涨跌额、涨跌幅、交易所标签清晰展示；
  - 支持后台定时自动轮询刷新（每 20 秒）与手动下拉/点击即时刷新。
- 🔍 **股票极速搜索与热门推荐**:
  - 支持输入代码或名称模糊搜索美股（如 `AAPL`, `TSLA`, `NVDA`）、港股（如 `0700.HK`, `9988.HK`）、A股（如 `600519.SS`, `000858.SZ`）；
  - 提供热门股票快捷推荐标签，一键加入自选。
- 📈 **大盘指数概览**: 顶部横向滚动展示标普500 (S&P 500)、纳斯达克 (NASDAQ)、道琼斯 (Dow Jones)、上证指数、深证成指、恒生指数等全球关键指数。
- 🎨 **人性化行情显示习惯**: 支持一键切换 **红涨绿跌 (国内/港股习惯)** 或 **绿涨红跌 (美股/国际习惯)**。
- 🛡️ **高可用离线容灾降级**: 即使在无网或服务端未配置状态下，内置合成演示引擎，确保 App 永不崩溃白屏。
- ⚡️ **Cloudflare Worker 边缘加速服务端**:
  - 基于 Hono + TypeScript 构建；
  - 汇聚全球股票行情与历史 K 线数据，统一 RESTful 结构；
  - 边缘多级缓存加速，响应延迟 `< 50ms`，免去跨域与频率限制困扰。

---

## 📁 目录结构

```text
stock-trading-system/
├── .github/
│   └── workflows/
│       ├── build-apk.yml            # GitHub Actions Android APK 自动编译工作流
│       └── deploy-cloudflare.yml    # GitHub Actions Cloudflare Worker 自动部署工作流
├── android/                         # Android 原生应用源码 (Kotlin + Jetpack Compose)
│   ├── gradle/wrapper/              # Gradle Wrapper 8.7 执行脚本与配置
│   ├── gradlew / gradlew.bat        # Gradle 执行命令
│   ├── build.gradle.kts             # 根构建配置
│   ├── settings.gradle.kts          # 模块与依赖源配置
│   └── app/
│       ├── build.gradle.kts         # App 模块配置 (Compose, Retrofit, Material3)
│       └── src/main/
│           ├── AndroidManifest.xml  # 清单文件 (联网权限)
│           ├── java/com/stockmarket/app/
│           │   ├── StockApp.kt      # 应用入口
│           │   ├── MainActivity.kt  # Compose Activity
│           │   ├── data/            # 实体模型、网络接口、本地缓存与仓储层
│           │   └── ui/              # 主题、Canvas图表组件、自选页、详情页、搜索页、设置页
│           └── res/                 # 图标、文字、颜色资源
├── server/                          # Cloudflare Worker 服务端源码
│   ├── wrangler.toml                # Cloudflare 配置文件
│   ├── package.json                 # 依赖配置
│   ├── src/
│   │   ├── index.ts                 # Hono API 路由定义
│   │   ├── services/stockService.ts # 股票行情与历史K线抓取解析
│   │   └── types.ts                 # TypeScript 数据类型定义
│   └── test/api.test.ts             # 单元自动化测试用例
└── README.md
```

---

## 🚀 第一步：推送到 GitHub（自动云端构建 APK）

按照您的需求，**无需在本地安装任何 Android SDK、Java 或 Gradle**，GitHub Actions 提供的 `ubuntu-latest` 运行环境中已经预装好所有编译环境。

### 1. 初始化 Git 并推送到您的 GitHub 仓库

在终端执行以下命令（将 `<YOUR_GITHUB_REPO_URL>` 替换为您在 GitHub 创建的新仓库地址）：

```bash
cd /workspace/stock-trading-system

# 初始化 git
git init
git add .
git commit -m "feat: initial commit of stock vision android app and cloudflare worker"

# 设置主分支并关联远程仓库
git branch -M main
git remote add origin <YOUR_GITHUB_REPO_URL>
git push -u origin main
```

### 2. 在 GitHub 下载编译好的 APK

1. 打开您推送的 GitHub 仓库页面，点击顶部的 **Actions** 标签栏；
2. 您会看到正在运行的 **Build Android APK** 工作流；
3. 构建过程一般仅需 2~3 分钟即可完成；
4. 构建成功后，点击进入该次运行记录，在页面底部的 **Artifacts** 区域即可找到并点击下载：
   - 📦 **StockVision-Debug-APK.zip**
5. 解压下载的 zip 包即可得到可以直接安装在 Android 手机或模拟器中的 `app-debug.apk`！

---

## ☁️ 第二步：部署 Cloudflare Worker 服务端

服务端负责从公开财经数据源提取并清洗股票实时价格与历史 K 线数据，并通过 Cloudflare 全球边缘节点进行就近缓存分发。

### 方式 A：通过本地 Wrangler 命令行一键部署（推荐）

如果本地已登录 Cloudflare，可在 `server` 目录下直接执行：

```bash
cd /workspace/stock-trading-system/server

# 登录 Cloudflare（如尚未登录）
npx wrangler login

# 部署至 Cloudflare Workers
npx wrangler deploy
```

部署完成后，终端会输出您的专属 Worker 访问域名，例如：
`https://stock-trading-worker.<your-subdomain>.workers.dev`

### 方式 B：通过 GitHub Actions 自动持续部署

若希望推送到 GitHub 后自动部署 Worker：
1. 打开您的 GitHub 仓库 -> **Settings** -> **Secrets and variables** -> **Actions**；
2. 新增 Secret: `CLOUDFLARE_API_TOKEN`（在 Cloudflare 控制台的 *My Profile -> API Tokens -> Create Token (选择 Edit Cloudflare Workers 模板)* 生成）；
3. （可选）新增 Secret: `CLOUDFLARE_ACCOUNT_ID`；
4. 之后每次修改 `server/` 代码推送到 `main` 分支时，GitHub Actions 的 **Deploy Cloudflare Worker** 工作流将自动触发部署。

---

## 📱 第三步：在 Android App 中配置服务端地址

1. 在手机上打开安装好的 **StockVision** 应用；
2. 点击右上角的 **设置 (齿轮图标)**；
3. 在 **行情服务端配置** 中填入您部署好的 Cloudflare Worker 网址（例如 `https://stock-trading-worker.xxx.workers.dev`）；
4. 点击 **保存并测试连接** 按钮，系统会即时检测健康状态并显示连接成功提示；
5. 返回主界面，自选股票与大盘指数将立即从您的 Cloudflare 服务端同步实时价格与 K 线！

> 💡 **提示**：如果暂未部署 Cloudflare Worker，App 内置智能离线兜底引擎，自选、搜索、以及 1D/5D/1M/6M/1Y/ALL 历史 K 线图与 MA 均线均可正常交互浏览与测试！

---

## 🛠️ 本地开发与单元测试 (可选)

### 运行 Cloudflare Worker 单元测试
```bash
cd server
npm test
```
*所有 6 个核心测试用例（状态健康检查、实时报价、自选批量查询、历史蜡烛图生成、股票模糊搜索、大盘指数）均已通过。*

### 本地启动 Worker 实时热重载调试
```bash
cd server
npm run dev
```

---

## 📄 授权与说明

本项目遵循 MIT 开源许可证。数据源仅供个人学习研究与技术交流，投资有风险，入市需谨慎。
