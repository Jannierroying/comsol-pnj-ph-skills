# COMSOL 6.3 案例源码

以下源码来自参考库的历史任务，公开版仅修改输出路径及建立输出目录。对应物理模型在作者本机已实际计算并得到用户可行性确认；路径适配后的源码在发布时只做 COMSOL 6.3 编译检查，未再次计算。

## 文件与顺序

- `pang-grin-fig2d/`：`PangGRINBuild.java` → 计算 `models/Pang2025_Fig2d_GRIN.mph` 中的 `std1` → `PangGRINDisplay.java`。
- `coaxial-pnj-chain-fig4/`：`PNJChainBuild.java` → 计算 `models/Xu2024_Fig4_PNJChain.mph` 中的 `std1` → `PNJChainDisplay.java`。Build 本身会求解空空气域作端口方向核验，尚未计算目标双圆柱。
- `edge-diffraction-trapezoid-fig1b/`：`EdgePHBuild.java` → 计算 `models/Xu2024_Fig1b_EdgeDiffraction_PH.mph` 中的 `std1` → `EdgePHDisplay.java`。
- `janus-lg-vortex-fig1/`：`JanusLGBuild.java` → `JanusLGSolve.java` → `JanusLGDisplay.java`。使用三维全波 BEM，原论文 FDTD 和纵向源近似的迁移差异见案例记录。
- 双椭圆内核扫描当前提供参数与方法记录；没有分发依赖历史 `.mph` 的旧脚本。

构建脚本会建立参数、几何、材料、物理接口、网格和研究，并保存待计算模型。计算可通过 GUI 的“计算”、COMSOL 原生批处理或用户已配置的 MCP 完成。显示脚本加载已经含解的模型生成 `ewfd.normE^2`、Rainbow 原生场图，并保存模型；不是目标研究的求解步骤。部分脚本写入解的基本核验记录，但不提取 FWHM、PH 弯曲角或手性分布。

## Windows 路径与批处理示例

将源码复制到本次任务文件夹，不在参考模型目录运行构建脚本。它会覆盖该输出目录下同名模型；显示脚本也会维护该案例绘图组。因此每次独立任务先指定新的输出目录，Build、Compute 和 Display 全程使用同一目录。

PowerShell 示例（用自己的 COMSOL 安装位置和任务输出位置替换示例）：

```powershell
$env:PNJPH_PROJECT_ROOT = 'E:\codex\Projects\my-pang-run'
$comsolBin = 'D:\COMSOL\COMSOL63\Multiphysics\bin\win64'
& "$comsolBin\comsolcompile.exe" '.\PangGRINBuild.java'
& "$comsolBin\comsolbatch.exe" -inputfile '.\PangGRINBuild.class' -batchlog '.\build.log'
# 在 GUI 打开 $env:PNJPH_PROJECT_ROOT\models\Pang2025_Fig2d_GRIN.mph，运行 std1 并保存。
& "$comsolBin\comsolcompile.exe" '.\PangGRINDisplay.java'
& "$comsolBin\comsolbatch.exe" -inputfile '.\PangGRINDisplay.class' -batchlog '.\display.log'
```

`PNJPH_PROJECT_ROOT` 指这一次任务目录；未设置则脚本报错。环境变量只在当前进程及子进程生效，不修改 Windows HOME 或全局用户目录。源码需要 COMSOL 6.3 对应模块和可用许可证；COMSOL 方法执行若因文件访问设置失败，在当前任务的受控偏好配置中按报错处理，不自动改动全局安全设置。判定成功要检查日志末尾、模型含解状态及实际输出，不能只看批处理退出码。

原始高密度二维案例可能消耗较多内存；案例记录给出原网格规模及求解器选择。降低网格密度或改变阶次后属于变体，需重新核验。目录里的源码不是独立安装的 COMSOL MCP 服务。

光线追迹源码位于 `comsol-pnj-ph-raytrace/assets/`，同样使用 `PNJPH_PROJECT_ROOT`，但应读取光线追迹 skill 的 gop / RayTracing 工作流，不能沿用上述波动光学研究设置。
