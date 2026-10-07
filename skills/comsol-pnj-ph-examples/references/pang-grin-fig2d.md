# GRIN-PANG-001：Pang 2025 图 2(d) 线性梯度微柱

> 公开版说明：下文的模型、哈希、运行记录与用户确认描述原作者本机的历史归档；这些含解 `.mph`、论文 PDF、原始图和日志未随本仓库分发。可复用源码已提供，路径改为环境变量，本次发布只做编译检查，未重新运行仿真。新环境的计算及确认状态应另行记录。

公开源码：[PangGRINBuild.java](../assets/pang-grin-fig2d/PangGRINBuild.java)、[PangGRINDisplay.java](../assets/pang-grin-fig2d/PangGRINDisplay.java)。使用步骤见 [源码运行说明](../assets/README.md)。

## 状态与确认依据

**用户已确认可行**，确认及归档日期：2026-10-05（Asia/Shanghai）。用户原话：“复现模型可行归档到可行的skill里面”。这明确指向刚交付的 Pang 图 2(d) 模型，不推广到其他论文、模型或扫描条件。

验证范围：本案例的实际求解、含解模型、基本场图及用户对模型可行性的确认。场图呈向负 y 偏移的 PNJ，与选定图的基本形态对应。没有验证论文全部定量指标，也没有完成单元阶次、计算区域与 PML 的完整收敛比较。

## 来源和交付物

来源：Pang 等，*Generation and modulation of off-axis photonic nanojets via gradient-index microcylinders*，Optics Letters 50(16), 4882–4885 (2025)，DOI `10.1364/OL.564490`，图 2(d) 和式 (1)。COMSOL 版本：6.3.0.290。

项目根目录：`PROJECTS_ROOT\2026-10-05_Pang2025_GRIN_Fig2d`。以下路径均相对该根目录，文件已在归档时核对存在。

- 唯一最终含解模型：`models/Pang2025_Fig2d_GRIN.mph`，222,220,808 字节。
- 模型 SHA-256：`C82A20130677ED494E55462AFF8AA306652AD37A4D7C6CD56BFC5CC25FDEABFB`。
- 全物理域场图：`figures/Pang2025_Fig2d_Field.png`；PNJ 局部场图：`figures/Pang2025_Fig2d_PNJ.png`。
- 源论文：`source/Pang2025.pdf`；文字提取：`source/paper_text.txt`。
- 参数与补充设置：`复现说明.md`；网格核验：`validation/settings_and_mesh.txt`。
- 求解完成记录：`validation/solve_status.json`；含解重载记录：`validation/computed_solution.txt`。
- 用户确认记录：`validation/user_confirmation.json`。`computed_solution.txt` 中的待确认状态属于归档前历史记录，以本确认记录和此案例状态为准。
- 构建脚本：`scripts/PangGRINBuild.java`；显示脚本：`scripts/PangGRINDisplay.java`；均有已编译 `.class`。
- 本次实际使用的批处理偏好目录：`runtime_prefs`。构建与显示日志位于 `logs`；最终显示成功日志为 `display_units.log`。

不把大型 `.mph`、论文或场图复制到 skill 目录；路径是索引，不是可任意覆盖的输出位置。

## 本案例的物理与数值设置

二维无限长圆柱的圆截面，中心 `(0,0)`，`R=7.5[um]`。真空波长 `lambda_ref=633[nm]`，空气 `n_bg=1`，`n1=1.72`，`delta_n=0.3`，`theta=0[deg]`；只算这一组，没有参数化扫描。

按原文式 (1)，组件变量为 `n_grin=n1-delta_n*(y+R)/(2*R)`。圆柱内部相对介电常数为 `n_grin^2`；底部、中心、顶部分别为 1.72、1.57、1.42。梯度是连续的空间材料函数，不采用均匀等效折射率或少数离散层。空气与 PML 材料均为 `n_bg^2`。

ewfd 二维面内电场，散射场形式；入射沿 +x，电场沿 y，`Eb={0,Eamp*exp(-i*k_bg*x),0}`，`k_bg=2*pi*n_bg*freq/c_const`。研究为 `Wavelength`，`plist=lambda_ref`。显示总场 `ewfd.normE^2`，Rainbow，单位 `V^2/m^2`，使用 Selection 绘图属性排除 PML。

补充而非论文明确给出的设置：`Eamp=1[V/m]`、各向同性/无损/非磁性、`mu_r=1`、`sigma=0`；物理域 x 为 `[-8.766,11.298] um`、y 为 `[-8.766,8.766] um`；八个笛卡尔 PML 外围域，厚 `633 nm`。实际域选择：物理域 `[5,10]`，圆柱 `[10]`，PML `[1,2,3,4,6,7,8,9]`；改几何后重新读取命名选择，不能照抄域号。

物理域自由三角形，PML 映射四边形。一阶边单元是本次内存条件下的选择，论文未公开阶次。Size 设置 `h_max=lambda_ref/32`、`h_min=h_max/8`、增长率 1.15；实际三角形 2,497,960 个、四边形 125,824 个，最大三角形边长 26.8766 nm，小于论文 `lambda/20=31.65 nm`。这个边长核验不等于数值收敛验证。

PARDISO 直接求解器，`ooc=on`、`oocmemory=2048` MB；后者不是整个进程的内存上限。MCP 实际计算耗时 320.400886 秒；保存后重载，`sol1.getSize()=[4000618,1]`，一组解，`getPNames()=[lambda0]`、`getPVals()=[6.33e-7]`，实际波长 633 nm。

## 可复用的方法与差异

本例为连续梯度圆柱，不是双椭圆核壳模板。可沿用连续材料函数、命名域选择、面内散射场、波长域、PML、细网格核验与基本场图的组织方式；尺寸、折射率、偏振、单元阶次和内存参数均需按新任务检查。

- `PangGRINBuild.java` 从零构建并核验网格、保存未计算模型，不运行研究；由 MCP 载入后调用 `study_solve_async({})` 实际求解并保存。
- `PangGRINDisplay.java` 只加载已计算的 `.mph`，核验解并生成完整场图与局部图，不重新计算研究。它清理其他绘图组、维护两组本例场图及图像导出节点。
- 当前工具不能用原生 MCP 写入全部材料/PML/研究/绘图节点，所以上述流程组合了 MCP 与 COMSOL 官方 Java API。
- 当前 Surface 不能直接调用 `.selection().named(...)`；本例通过 `.create("sel1","Selection")` 添加绘图属性，再设置实际物理域编号。
- 当前实际二维 view axis 属性与手册有差异，不支持本例尝试的 `auto`、`manuallimits` 和 `equal`。成功脚本使用 `viewscaletype=none` 与 `xmin/xmax/ymin/ymax`；新版本先核对实际 properties。
- 单波长无外部扫描时，`getParamNames()/getParamVals()` 返回空数组；不能据此认定没有解。检查 `isEmpty()`、`getSize()`，并用 `getPNames()/getPVals()`核对实际波长。
- COMSOL 批处理可能在 Java 出错时仍以退出码 0 结束并保存检查点，必须读最终成功日志和真实交付物。本例早期错误日志保留，不代表最终模型求解失败。

原文 21.88（a.u.）是文献色标参照，未作为本次实算峰值或误差记录。`Eamp=1 V/m` 时，normE² 数值与相对于入射电场平方的归一化数值一致，单位仍是 `V²/m²`，不是绝对功率密度。

## 以后如何使用

例如：“参考 GRIN-PANG-001，保持其材料梯度，改成波长 532 nm，计算并生成场图。”先读本记录和对应模式，另建 `PROJECTS_ROOT` 下的新任务目录，从原案例复制后修改。公开版两份 Java 的 ROOT 由 `PNJPH_PROJECT_ROOT` 设置；执行前检查该环境变量和输入/输出路径，更新波长、半径断言、视图边界及单位，重新检查网格和 PML。查阅或归档无需执行脚本，不覆盖已确认模型。

旋转梯度或更换液体环境应回到论文核对相应方程和参数；当前 `theta` 参数仅记录零度条件，不参与 `n_grin` 表达式，直接改它不会旋转材料分布。新条件的求解结果另行验证，不能继承此案例的用户确认。
