# DE-IR-001：同心双椭圆内核旋转

> 公开版说明：本条目记录原作者已确认的 15 组内核扫描。下文的含解模型、历史脚本、图和日志未随公开版分发；本条目作为参数及建模参考，不能直接打开历史路径。重建时使用多臂专例和复现模式，不将新建模型标记为已经验证。

## 状态与证据

计算记录日期：2026-10-04。**用户已确认可行**，确认日期：2026-10-05。用户原话：“对于之前的二维同心双椭圆核壳内核旋转扫描这个模型也可行”。确认对象是本案例同一模型内保存的15组内核旋转扫描；确认记录见项目 `validation/user_confirmation.json`。此次归档更新状态和确认依据，不重新运行研究。

求解验证范围：研究成功结束，实际解的外层参数数量15，表面表达式 `ewfd.normE^2` 和 Rainbow 可读，原生 PNG 已生成。不是论文全部结果、PNJ/PH臂数或网格收敛的验证；定量分析仍按用户要求暂停。

## 交付物

项目根目录：`PROJECTS_ROOT\2026-10-04_DualEllipse_Paper_Reproduction`。

- 含全部15组解：`models/DualEllipse_InnerRotation_solved.mph`，1,612,808,201 字节（约1.61GB）。
- 模型 SHA256：`9FD5ED99E603362F6E535C7CFD33609052BEA57E1882AC8AF18E65A99D701900`。
- 未计算的设置查看版：`models/DualEllipse_Parametric_Settings.mph`，不能当成含解模型。
- 原生场图：`figures/basic_field/InnerRotation_theta30_shell60.png`，展示内核30°。
- 计算与表达式核验：`validation/inner_rotation_completed.txt`。
- 用户确认记录：`validation/user_confirmation.json`，仅确认此扫描模型可行性；其他分析任务的授权范围单独记录。
- 状态及暂停范围：`workflow_state.json`、`AGENTS.md`。
- 原生绘图日志：`logs/inner_rotation_field.log`。
- 设置脚本：`scripts/DualEllipseCoreSettings.java`；绘图脚本：`scripts/DualEllipseCoreField.java`。
- 来源：`source/最终版.pdf`；原文件 用户提供的原始 PDF（未随公开版分发）。

上述相对路径均相对项目根目录。旧53个单独模型、36组联合扫描脚本及中间文件不属于当前参考结果。

## 模型条件

二维无限长微柱截面；内外椭圆中心均(0,0)，角度从+x逆时针。原文局部角度定义有冲突，此实现按后文与图3—7采用 theta_core 为内核，theta_shell 为外壳。

- 真空波长 `lambda_ref=532[nm]`；背景、核、壳折射率分别1、1.5、1.29。
- 壳长半轴 `a_shell=6*lambda_ref`，短半轴 `b_shell=a_shell/2`；核长半轴 `a_core=s_core*a_shell`，短半轴 `b_core=a_core/2`。
- 仅扫描 `theta_core`：`range(30[deg],5[deg],100[deg])`，参数单位栏 `deg`；15组全解存同一模型。
- 固定 `theta_shell=60[deg]`、`s_core=0.6`。排除单椭圆对照；外壳旋转和S扫描不属于本次研究。
- ewfd 接口、面内 Ex/Ey、散射场；入射沿+x，背景电场 `{0,Eamp*exp(-i*k_bg*x),0}`，`k_bg=2*pi*n_bg*freq/c_const`。研究类型 Wavelength，波长列表 `lambda_ref`。
- 补全假设：Eamp=1 V/m，各向同性、无损、非磁性，epsilon_r=n²、mu_r=1、sigma=0。不是论文明确报告的激光功率或色散模型。
- 补全计算区域：物理域 x为[-8λ,20λ]、y为±7λ，外围Cartesian多项式PML厚λ；物理域自由三角形、PML映射网格，二阶边元。
- 网格 Size 设置 hmax=λ/21、hmin=hmax/8、hgrad=1.2。hmax设置值并非所有实际边长的严格上限：此前检查存在约33—36nm边长，不能宣称已满足论文所有边长<λ/20或完成收敛。用户验证模型可行也不能消除此限制。

复用时读实际命名选择。当前数值域号只是该几何的结果，不作为新模型的通用域号；独立旋转后须重新检查内核包含于外壳，不能只凭S<1。

## 本次有效的执行方法

通过官方Java API配置研究、收集全部参数解和场图，MCP载入后实际研究求解，再保存含解模型；原生Java API生成基本场图。不属于纯MCP写入全部节点。

`DualEllipseCoreSettings.java` 依赖已有设置模型，清除已有解/数据集并重新配置，不是从零建模脚本。`DualEllipseCoreField.java` 依赖已有含解模型、pgField/surf1，固定期待15解，并清理其他绘图组。二者 ROOT 均硬编码原项目，**不得直接原地执行作为新任务**。先复制所需文件、修改ROOT/扫描计数/输出路径并核对副作用；查阅案例无需执行脚本。

本次绘图组参数层级为 `[lambda0, theta_core]`，`looplevel=[1,1]`选首个波长和内核30°；当前6.3 PlotGroup2D不支持`looplevelinput`，须读properties和真实层级再设置。不要把两层假设推广到别的研究。

扫描数量通过实际解 `getSolutioninfo().getOuterSolnum().length` 核验；`getParamVals()`在本次只返回最后角度100°的弧度值，不能据此判断仅算1组或已列出全部角度。

本次MCP仅一个研究，调用 `study_solve_async({})` 成功；传Java标签std1被包装层当显示名称而失败。研究/数据集应使用各工具实际返回的名称，或用Java API真实标签查询。进度曾长期停在0.3，不用它宣称实际完成30%；等待成功状态与日志确认完成。

MCP保存后响应显示名称可能变化而model_list键保留旧名；需要操作时重读列表或使用当前模型。批处理也要读日志及交付物，不能只以进程退出码0判断Java调用成功。

## 与原流程的差异及可沿用部分

本案例以同一模型的外部参数扫描替代逐角度模型；只有一个扫描参数，保留全部解；不自动扩大到完整论文的壳角/S/单椭圆研究。场图直接normE²、Rainbow，计算和基本显示已执行，其他分析独立暂停。通用扫描/单位/保存规则已回写共享参考；上述几何、固定值、参数层级和包装层行为保留为案例条件。

类似同心椭圆案例可参考几何命名、核壳材料、背景场、外部扫描和全部解保存方法。圆形PNJ、三维微粒或金属/各向异性模型仍采用对应模式，不照搬这些材料、尺寸和偏振。
