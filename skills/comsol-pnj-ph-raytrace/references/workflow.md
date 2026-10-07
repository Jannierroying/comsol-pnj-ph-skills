# COMSOL 6.3 MCP/API 光线追迹实操

## 精确API与单位

- 物理接口：`physics().create("gop","GeometricalOptics","geom1")`；研究：`study("std1").create("rt","RayTracing")`。确认实际模块可用，插件文件存在不等于许可证有效。
- 释放节点：`create("relg1","ReleaseGrid",-1)`；不是ReleaseFromGrid。二维释放坐标x0有两项，但方向L0在本机API仍须三项，如`{"1","0","0"}`。
- 单色设置：RayProperties节点op1的`RayPropertySpecification=SpecifyVacuumWavelength`、`lambda0=lambda_ref`。范例默认已是此选项；新模型明确设定/读取，并在实际解验证`gop.lambda0/1[nm]`，不要只核对全局参数。
- 材料采用RefractiveIndex属性组，分别设n及ki；默认mp1的n_mat=from_mat。epsilon=n²并不能替代这里需要的折射率材料属性。外部未网格域next也须对应背景。
- 二维面内偏振：InitialPolarizationType=FullyPolarized、axy0=1、az0=0，对+x入射对应E沿y；不要使用三维a10/a20替代二维字段。其他偏振按实际要求修改。
- 模式：IntensityComputation=ComputePower。平面波复电场振幅约定下 `I_inc=0.5*n_bg*epsilon0_const*c_const*abs(Eamp)^2`；二维 `Psrc_2d=I_inc*释放口径高度`（W/m），三维总功率为照明面功率积分（W）。本公式仅用于相应非磁性无损背景，不通用于任意光源。
- 默认matd1为MaterialDiscontinuity；弱反射阈值Qth及次级射线槽数分别控制分支截止和可分配数量。多次全反射不必另分配反射分支。外界设置WallCondition=Disappear，仅选外边界，不能误选核壳界面。
- UseGeometryNormals=true可使用可解析曲面的几何法向；其有效性与几何和变形情况有关。射线网格用于界面定位/材料取值，不自动等同于波动光学网格收敛。

## 配置、计算、显示分开

Java配置范例在本次项目编译，使用本机COMSOL自带comsolcompile.exe；用comsolbatch.exe载入.class，`-prefsdir`指向任务自己的runtime_prefs，`-nosave`时仍由脚本显式m.save保存。日志与实际文件都要检查，退出码0不保证Java内部调用无错。范例createAutoSequences不会求解，后续必须运行研究。

MCP model_load载入本次设置模型，study_list核对研究，再study_solve_async运行。单研究可省略study_name；不要把API标签std1当成包装层显示名称。以study_wait/完成状态核验，单次等待不超过60秒；先model_save保存含解文件，随后Java加载它进行轨迹图配置。保存后的模型列表键可能未随显示名更新，操作前核对。

Ray数据集关联实际solver及gop；只有Solution数据集不保证已生成射线图。RayTrajectories配Color，primary视图用RayTrajectoriesFilter的type=primary；它仍可能包含初始射线本身的全反射，不能称“完全无反射”。完整视图保留次级分支。

二维本机范例的gop.Q为功率变量，Qray0为单位厚度初始功率。采用单位深度1[m]解释时，相对功率用 `gop.Q/(Qray0*1[m])`，Color单位设1，Rainbow。单独gop.Q/Qray0会有长度量纲，本次曾自动以μm显示为10^6，已修正。不要将1[m]因子推广到三维或不同Q定义；先核对分子分母单位，或直接按实际初始单条射线功率归一化。

本次研究设置 `timestepspec=specifylength`、`llist=range(0[um],0.05[um],25[um])`、charvel=c_const；这是输出时间对应的特征长度刻度，不是每条射线真实几何路程或累积光程。501时刻、约83.391fs仅属本案例。其他模型按目标路径给终止值，不能让轨迹被观察区/时间截断却声称完整。

## 验证与数据

用Ray数值评估节点读取gop.Q、qx/1[um]、qy/1[um]；getReal返回[射线][解时刻]，先核对维度与solver时刻列表。首时刻功率为正且已释放的射线统计初始条数；一度功率为正的射线可统计曾释放条数。本统计只适用于本范例非零功率且t=0一次释放，其他释放方式另定统计法。

核验实际波长、初始总功率与设定值、释放位置与方向、偏振、材料选择、已用次级槽和求解警告（API可读getWarningMessage）。预分配槽数不是实际已释放数。取图必须使用完整轨迹所需的保存时刻；导出指定外层参数解，明确功率/单位厚度约定。不要把getPVals等时间数组用于判断参数扫描组合数。

参考：[Release](https://doc.comsol.com/6.3/doc/com.comsol.help.roptics/roptics_ug_optics.6.13.html)、[Material Discontinuity](https://doc.comsol.com/6.3/doc/com.comsol.help.roptics/roptics_ug_optics.6.07.html)、[Geometrical Optics settings](https://doc.comsol.com/6.3/doc/com.comsol.help.roptics/roptics_ug_optics.6.03.html)。本机fresnel_rhomb示例用于核对API；导出Java需m.save(path,"java")，只用m.save(path.java)仍会写二进制mph。
