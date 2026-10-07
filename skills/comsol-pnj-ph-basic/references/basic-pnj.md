## 基础 PNJ 操作示例：二维圆柱＋波长域

本节是 GUI 建模参数与节点示例，采用二维均匀介质圆柱，表示沿 z 方向无限延伸的微柱。入射光沿 +x 传播、电场沿 y 方向。同一组参数已有一次非 GUI 实算，见 [MCP/API 实操](mcp-workflow.md)；新建或改参模型仍须重新求解验证。

1. **模型向导。**选择二维，添加“光学 → 波动光学 → 电磁波，频域（Electromagnetic Waves, Frequency Domain，ewfd）”，研究选择“波长域（Wavelength Domain）”。已有模型可添加波长域研究并使用它求解。研究改为波长域，物理场接口仍使用 ewfd。

2. **全局定义 → 参数。**录入以下参数，不另行手动指定求解频率。lambda_ref 是固定参考波长，用于单波长输入及几何尺寸定义。

   ```text
   lambda_ref    532[nm]
   n_bg          1
   n_particle    1.5
   d_particle    4*lambda_ref
   r_particle    d_particle/2
   Eamp          1[V/m]
   t_pml         lambda_ref/n_bg
   h_air         lambda_ref/10
   h_particle    lambda_ref/(12*n_particle)
   ```

   该例圆柱直径 2.128 μm、半径 1.064 μm，粒子折射率 1.5、背景折射率 1。Eamp 用于归一化。

3. **几何。**长度单位设为 μm。添加矩形，宽 28*lambda_ref、高 18*lambda_ref，位置基准为左下角，坐标为 (-9*lambda_ref,-9*lambda_ref)。在矩形的 Layers 中添加厚度 t_pml 的一层，勾选左、右、上、下四侧，外围层用于 PML。再添加圆，圆心 (0,0)、半径 r_particle。形成联合体，保留内部边界并构建全部几何。中央矩形中圆外的区域为空气，外围层与粒子分开。

4. **定义与 PML。**建议为粒子、物理空气域和 PML 建立命名选择；未建立时逐一核对实际域号。添加 Perfectly Matched Layer，只选择外围层域，采用 Cartesian 类型，核对拉伸设置。在本例已构建的 10 域几何中，外围域为 1–4、6–9，空气域为 5、圆域为 10；只要几何变化，就重新检查域号。在组件的 Variables 中定义：

   ```text
   k_bg = 2*pi*n_bg*freq/c_const
   ```

   单位为 1/m。freq 是研究当前求解频率；这样背景波随波长域输入或扫描自动变化。k_bg 属于组件变量，不写入固定的全局参数列表。

5. **材料。**粒子空白材料只选择圆域，相对介电常数 n_particle^2=2.25；背景材料选择空气和 PML 域，相对介电常数 n_bg^2=1。两种材料的相对磁导率均为 1，电导率均为 0。在 ewfd 的 Wave Equation, Electric 中将电位移场模型改为 Relative permittivity，并从材料读取。

6. **物理场。**ewfd 主节点选择 Scattered field、User defined 背景场、In-plane vector，面外波数设为 0。背景电场填写：

   ```text
   Ex = 0
   Ey = Eamp*exp(-i*k_bg*x)
   Ez = 0
   ```

   粒子与空气内部界面保持介质连续条件。检查默认 PEC 仅作用于 PML 最外边界；此例已由背景场激励，不再添加第二个入射源。

7. **网格。**若采用显式尺寸方案，需在圆域、物理空气域的 Size 节点分别填入最大单元尺寸 `h_particle`（约 29.6 nm）、`h_air`（53.2 nm），并为 PML 的 Distribution 节点指定厚度方向层数，例如先取 8 层；仅定义这些全局参数不会改变网格。此前成功实算采用自动网格档位 `autoMeshSize(3)`，并用档位 `2` 做局部对照，未落实上述显式最大尺寸或固定 8 层。两种网格方案不要混称，选用哪种就记录哪种并验证。

8. **波长域研究与求解。**点击“研究 → 波长域”，在 Study Settings 中将 Wavelength unit 设为 nm，在 Wavelengths 栏填写带单位的参数 lambda_ref，等于 532[nm]。采用自动生成的研究求解器；当前频率由软件自动换算为 c_const/当前真空波长，无须输入 freq0。用独立副本将 `n_particle` 改为 1 进行无粒子对照，检查总场约为 1、散射场很小及能流指向 +x；保留 n=1.5 的主模型与解，不让对照覆盖它。[COMSOL 6.3 波长域设置](https://doc.comsol.com/6.3/doc/com.comsol.help.comsol/comsol_ref_solver.36.105.html)

9. **总场增强图。**在组件 Variables 中定义：

   ```text
   G = (abs(ewfd.Ex)^2+abs(ewfd.Ey)^2)/Eamp^2
   ```

   建立二维绘图组与表面图，绘制 G；优先仅显示物理域并放大粒子右侧。若导出图仍包含 PML，测量时明确排除其域。总场分量不再重复叠加背景场。同时计算时间平均坡印廷矢量，检查粒子外部的聚焦和传播方向。

10. **焦点与束宽。**建立 Cut Line 2D，轴向截线从 (r_particle+5[nm],0) 到 (12*lambda_ref,0)，绘制 G(x)，找到粒子外部主峰及位置 x_f。再建立从 (x_f,-2*lambda_ref) 到 (x_f,2*lambda_ref) 的横向截线，绘制 G(y)，用主峰两侧最近半高交点求 FWHM。焦距取 x_f-r_particle。有效长度按明确的强度阈值测量主峰对应的外部连续区间，半高交点不完整时记录为无法完整测量。

11. **收敛与扫描。**先将空气和粒子网格尺寸分别缩小至原来的 2/3、1/2，并改变空气域或 PML 厚度，比较峰值、焦点位置和 FWHM。基准模型稳定后，分别扫描 d_particle=3*lambda_ref、4*lambda_ref、5*lambda_ref，以及 n_particle=1.4、1.5、1.6。研究的单波长输入仍为 lambda_ref。需要光谱扫描时，在波长域的 Wavelengths 栏输入例如 range(450,10,650)，单位 nm；这是自行提出的探索范围。此时 lambda_ref 和实际粒子尺寸保持固定，k_bg 使用当前 freq 自动更新，色散材料也使用当前波长或频率，网格按最短波长核查。

12. **保存与导出。**先在 `PROJECTS_ROOT` 下新建本次专用文件夹，把含解 `.mph`、脚本、增强图、截线数据及验证记录存入其中。注明每组结果的实际真空波长、几何参数、材料和测量定义；未实际提取的焦距、FWHM 或有效长度不可在交付清单中称为已完成。重新载入最终 `.mph` 检查研究、解和关键结果。用户指定其他目录时以其要求为准。
