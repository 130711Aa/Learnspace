package com.lernspace.app.core.mimolab

/**
 * Curated, reverse-engineered STEM simulation engine presets for Mimo Lab.
 * Built with Clean White Laboratory theme, ResizeObserver, DPR auto-scaling,
 * touch/pointer drag handles, and zero external CDN dependencies.
 */
object MimoEnginePresets {

    fun getPresetSimulations(): List<MimoSimulation> {
        return listOf(
            MimoSimulation(
                id = "starter_ohm_circuit",
                title = "Rangkaian Listrik & Hukum Ohm",
                subject = MimoSubject.ELEKTRO,
                conceptTopic = "Rangkaian Listrik & Hukum Ohm",
                description = "Simulasi sirkuit listrik interaktif dengan baterai, saklar, lampu berfilamen menyala, aliran partikel elektron riil, serta perhitungan I = V/R dan daya P = V·I.",
                authorName = "Mimo STEM Team",
                likesCount = 215,
                htmlContent = getOhmCircuitHtml()
            ),
            MimoSimulation(
                id = "starter_snell_refraction",
                title = "Pembiasan Cahaya & Hukum Snellius",
                subject = MimoSubject.FISIKA,
                conceptTopic = "Optika Geometri & Gelombang Cahaya",
                description = "Eksplorasi pembiasan sinar laser melintasi antarmuka dua medium (Udara, Air, Kaca, Intan), garis normal, busur derajat, dan pemantulan sempurna (TIR).",
                authorName = "Mimo STEM Team",
                likesCount = 194,
                htmlContent = getSnellRefractionHtml()
            ),
            MimoSimulation(
                id = "starter_friction_inclined_plane",
                title = "Gaya Gesek pada Bidang Miring",
                subject = MimoSubject.FISIKA,
                conceptTopic = "Mekanika Klasik & Gaya Normal/Gesek",
                description = "Eksplorasi interaktif gerak benda di atas bidang miring dengan variasi sudut elevasi, koefisien gesek statis/kinetis, dan visualisasi uraian vektor gaya real-time.",
                authorName = "Mimo STEM Team",
                likesCount = 184,
                htmlContent = getFrictionSimulationHtml()
            ),
            MimoSimulation(
                id = "starter_hooke_spring",
                title = "Hukum Hooke & Elastisitas Pegas",
                subject = MimoSubject.FISIKA,
                conceptTopic = "Elastisitas & Gerak Harmonik Sederhana",
                description = "Eksperimen pegas gantung laboratorium dengan massa beban interaktif, penggaris ukur Δx, grafik linier F vs Δx, energi potensial regangan, dan getaran harmonik.",
                authorName = "Mimo STEM Team",
                likesCount = 178,
                htmlContent = getHookeSpringHtml()
            ),
            MimoSimulation(
                id = "starter_projectile_motion",
                title = "Gerak Parabola & Lintasan Peluru",
                subject = MimoSubject.FISIKA,
                conceptTopic = "Kinematika 2D & Gravitasi",
                description = "Visualisasi dinamis gerak peluru dengan sudut tembak interaktif, variasi gravitasi planet, efek hambatan udara, serta jejak lintasan multi-shot.",
                authorName = "Mimo STEM Team",
                likesCount = 162,
                htmlContent = getProjectileSimulationHtml()
            ),
            MimoSimulation(
                id = "starter_bohr_atom_shells",
                title = "Model Atom Bohr & Kulit Elektron",
                subject = MimoSubject.KIMIA,
                conceptTopic = "Struktur Atom & Konfigurasi Elektron",
                description = "Visualisasi struktur atom unsur periodik Z=1..36 dengan orbit kulit elektron konsentris K, L, M, N (2n²), konfigurasi elektron, dan penentuan elektron valensi.",
                authorName = "Mimo STEM Team",
                likesCount = 156,
                htmlContent = getBohrAtomHtml()
            ),
            MimoSimulation(
                id = "starter_punnett_genetics",
                title = "Genetika Mendel & Kotak Punnett",
                subject = MimoSubject.BIOLOGI,
                conceptTopic = "Genetika & Pewarisan Sifat",
                description = "Kalkulator dan visualisator persilangan genetika monohibrid (3:1) dan dihibrid (9:3:3:1) dengan kartu fenotipe warna, rasio genotipe, dan diagram probabilitas.",
                authorName = "Mimo STEM Team",
                likesCount = 142,
                htmlContent = getPunnettGeneticsHtml()
            ),
            MimoSimulation(
                id = "starter_riemann_sum_integral",
                title = "Aproksimasi Luas Integral Riemann",
                subject = MimoSubject.KALKULUS,
                conceptTopic = "Kalkulus Integral",
                description = "Visualisasi dinamis partisi luas kurva f(x) dengan metode Jumlah Riemann Kiri, Kanan, Titik Tengah, dan Trapesium serta batas interval interaktif.",
                authorName = "Mimo STEM Team",
                likesCount = 138,
                htmlContent = getRiemannSumSimulationHtml()
            ),
            MimoSimulation(
                id = "starter_carnot_cycle_thermo",
                title = "Siklus Mesin Kalor Carnot",
                subject = MimoSubject.TERMODINAMIKA,
                conceptTopic = "Termodinamika & Efisiensi Termal",
                description = "Diagram interaktif P-V 4 tahap Carnot (isotermal & adiabatik) disertai animasi silinder piston gerak gas dan perhitungan efisiensi teoritis.",
                authorName = "Mimo STEM Team",
                likesCount = 115,
                htmlContent = getCarnotCycleSimulationHtml()
            ),
            MimoSimulation(
                id = "starter_acid_base_titration",
                title = "Titrasi Asam-Basa & Kurva pH",
                subject = MimoSubject.KIMIA,
                conceptTopic = "Kimia Larutan & Titrimetri",
                description = "Simulasi buret laboratorium dan labu Erlenmeyer dengan perubahan warna indikator real-time, asam kuat vs lemah, serta kurva titrasi pH ekuivalen.",
                authorName = "Mimo STEM Team",
                likesCount = 97,
                htmlContent = getAcidBaseTitrationHtml()
            )
        )
    }

    fun getOhmCircuitHtml(): String = """
<!DOCTYPE html>
<html lang="id">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; -webkit-tap-highlight-color: transparent; }
  html, body { position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; margin: 0; padding: 0; overflow: hidden; display: flex; flex-direction: column; background: #FFFFFF; color: #0F172A; user-select: none; -webkit-user-select: none; }
  #canvas-container { flex: 1 1 0%; min-height: 0; position: relative; width: 100%; background: #FFFFFF; overflow: hidden; }
  canvas { position: absolute; top: 0; left: 0; width: 100%; height: 100%; display: block; touch-action: none; }
  #glass-hud { position: absolute; top: 14px; left: 14px; background: rgba(255, 255, 255, 0.94); backdrop-filter: blur(8px); border: 1px solid #E2E8F0; border-radius: 12px; padding: 10px 14px; box-shadow: 0 4px 14px rgba(0,0,0,0.06); font-size: 12px; line-height: 1.6; max-width: 320px; pointer-events: none; z-index: 5; }
  .hud-row { display: flex; justify-content: space-between; gap: 12px; }
  .hud-val { font-weight: 700; color: #D97706; font-family: ui-monospace, Menlo, Consolas, monospace; }
  #control-dock { flex-shrink: 0; background: #F8FAFC; border-top: 1px solid #E2E8F0; padding: 12px 18px; display: flex; flex-direction: column; gap: 10px; z-index: 10; }
  .sliders-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 12px; }
  .control-group { display: flex; flex-direction: column; gap: 4px; }
  .control-label { display: flex; justify-content: space-between; font-size: 11px; font-weight: 600; color: #64748B; }
  .val-badge { font-weight: 700; color: #D97706; font-family: ui-monospace, Menlo, monospace; }
  input[type="range"] { width: 100%; height: 26px; accent-color: #D97706; cursor: pointer; }
  .actions-row { display: flex; align-items: center; justify-content: space-between; gap: 10px; border-top: 1px solid #E2E8F0; padding-top: 8px; }
  .btn-group { display: flex; gap: 8px; }
  button { padding: 8px 16px; font-size: 12px; font-weight: 600; border-radius: 8px; cursor: pointer; border: 1px solid #CBD5E1; background: #FFFFFF; color: #0F172A; }
  button.active { background: #0F172A; color: #FFFFFF; border-color: #0F172A; }
  .formula-box { font-family: ui-monospace, Menlo, Consolas, monospace; font-size: 11px; color: #334155; background: #FFFFFF; border: 1px solid #E2E8F0; border-radius: 6px; padding: 6px 12px; }

  /* ── Studio Workbench Layout Upgrade ── */
  html, body {
    position: fixed !important;
    inset: 0 !important;
    width: 100vw !important;
    height: 100vh !important;
    margin: 0 !important;
    padding: 0 !important;
    overflow: hidden !important;
    display: flex !important;
    flex-direction: row !important;
    background: #FFFFFF !important;
    color: #0F172A !important;
    user-select: none !important;
    -webkit-user-select: none !important;
  }
  #canvas-container, #main-container {
    flex: 1 1 0% !important;
    min-width: 0 !important;
    height: 100% !important;
    position: relative !important;
    background: #FFFFFF !important;
    overflow: hidden !important;
  }
  #control-dock, #inspector-sidebar {
    width: 340px !important;
    max-width: 35vw !important;
    flex-shrink: 0 !important;
    height: 100% !important;
    background: #F8FAFC !important;
    border-left: 1px solid #E2E8F0 !important;
    border-top: none !important;
    padding: 20px 18px !important;
    display: flex !important;
    flex-direction: column !important;
    gap: 14px !important;
    overflow-y: auto !important;
    z-index: 10 !important;
    box-shadow: -4px 0 16px rgba(0,0,0,0.02) !important;
  }
  .sliders-grid {
    display: flex !important;
    flex-direction: column !important;
    gap: 12px !important;
  }
  .control-group {
    display: flex !important;
    flex-direction: column !important;
    gap: 6px !important;
    background: #FFFFFF !important;
    border: 1px solid #E2E8F0 !important;
    border-radius: 10px !important;
    padding: 10px 12px !important;
    box-shadow: 0 1px 3px rgba(0,0,0,0.02) !important;
  }
  .actions-row {
    display: flex !important;
    flex-direction: column !important;
    gap: 10px !important;
    border-top: 1px solid #E2E8F0 !important;
    padding-top: 14px !important;
    margin-top: auto !important;
  }
  .btn-group {
    display: flex !important;
    gap: 8px !important;
    width: 100% !important;
  }
  .btn-group button {
    flex: 1 !important;
  }
  .formula-box, .theory-card {
    font-family: ui-monospace, Menlo, Consolas, monospace !important;
    font-size: 11px !important;
    color: #0369A1 !important;
    background: #EFF6FF !important;
    border: 1px solid #BFDBFE !important;
    border-radius: 8px !important;
    padding: 10px 12px !important;
    line-height: 1.5 !important;
    word-break: break-word !important;
  }

</style>
</head>
<body>
<div id="canvas-container">
  <canvas id="simCanvas"></canvas>
  <div id="glass-hud">
    <div class="hud-row"><span>Status Saklar:</span><span id="hudSwitch" class="hud-val">Tertutup (Arus Mengalir)</span></div>
    <div class="hud-row"><span>Tegangan (V):</span><span id="hudVolt" class="hud-val">9.0 V</span></div>
    <div class="hud-row"><span>Hambatan (R):</span><span id="hudRes" class="hud-val">10.0 Ω</span></div>
    <div class="hud-row"><span>Kuat Arus (I):</span><span id="hudCurr" class="hud-val">0.90 A</span></div>
    <div class="hud-row"><span>Daya Lampu (P):</span><span id="hudPower" class="hud-val">8.10 W</span></div>
  </div>
</div>
<div id="control-dock">
  <div class="sliders-grid">
    <div class="control-group">
      <div class="control-label"><span>Tegangan Baterai (V)</span><span class="val-badge" id="valVolt">9.0 V</span></div>
      <input type="range" id="rangeVolt" min="1.5" max="24.0" step="0.5" value="9.0">
    </div>
    <div class="control-group">
      <div class="control-label"><span>Hambatan Lampu (R)</span><span class="val-badge" id="valRes">10.0 Ω</span></div>
      <input type="range" id="rangeRes" min="2.0" max="50.0" step="1.0" value="10.0">
    </div>
    <div class="control-group">
      <div class="control-label"><span>Jumlah Lampu</span><span class="val-badge" id="valBulbs">1 Lampu</span></div>
      <input type="range" id="rangeBulbs" min="1" max="3" step="1" value="1">
    </div>
    <div class="control-group">
      <div class="control-label"><span>Arah Aliran</span><span class="val-badge" id="valFlow">Elektron (e⁻)</span></div>
      <div class="btn-group">
        <button id="btnSwitch" class="active" style="flex:1;">Buka Saklar</button>
      </div>
    </div>
  </div>
  <div class="actions-row">
    <div class="btn-group">
      <button id="btnReset">Reset Default</button>
    </div>
    <div class="formula-box" id="formulaPill">
      Hukum Ohm: I = V / R = 9.0 V / 10.0 Ω = 0.90 A | P = V · I = 8.10 W
    </div>
  </div>
</div>

<script>
(function() {
  const container = document.getElementById('canvas-container');
  const canvas = document.getElementById('simCanvas');
  const ctx = canvas.getContext('2d');

  let width = 600, height = 400;
  function resize() {
    const w = container.clientWidth || 600;
    const h = container.clientHeight || 400;
    const dpr = window.devicePixelRatio || 1;
    canvas.width = Math.round(w * dpr);
    canvas.height = Math.round(h * dpr);
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    width = w;
    height = h;
  }
  window.addEventListener('resize', resize);
  if (window.ResizeObserver) {
    new ResizeObserver(resize).observe(container);
  }
  resize();

  // Params
  let voltage = 9.0;
  let resistance = 10.0;
  let numBulbs = 1;
  let isSwitchClosed = true;
  let electronPhase = 0;

  // DOM
  const rangeVolt = document.getElementById('rangeVolt');
  const rangeRes = document.getElementById('rangeRes');
  const rangeBulbs = document.getElementById('rangeBulbs');
  const valVolt = document.getElementById('valVolt');
  const valRes = document.getElementById('valRes');
  const valBulbs = document.getElementById('valBulbs');
  const hudSwitch = document.getElementById('hudSwitch');
  const hudVolt = document.getElementById('hudVolt');
  const hudRes = document.getElementById('hudRes');
  const hudCurr = document.getElementById('hudCurr');
  const hudPower = document.getElementById('hudPower');
  const formulaPill = document.getElementById('formulaPill');
  const btnSwitch = document.getElementById('btnSwitch');
  const btnReset = document.getElementById('btnReset');

  function updateHUD() {
    valVolt.textContent = voltage.toFixed(1) + ' V';
    valRes.textContent = resistance.toFixed(1) + ' Ω';
    valBulbs.textContent = numBulbs + ' Lampu (Seri)';

    const totalR = resistance * numBulbs;
    const current = isSwitchClosed ? (voltage / totalR) : 0;
    const power = isSwitchClosed ? (voltage * current) : 0;

    hudSwitch.textContent = isSwitchClosed ? 'Tertutup (Arus Mengalir)' : 'Terbuka (Rangkaian Terputus)';
    hudSwitch.style.color = isSwitchClosed ? '#059669' : '#DC2626';
    hudVolt.textContent = voltage.toFixed(1) + ' V';
    hudRes.textContent = totalR.toFixed(1) + ' Ω';
    hudCurr.textContent = current.toFixed(2) + ' A';
    hudPower.textContent = power.toFixed(2) + ' W';

    formulaPill.textContent = 'Hukum Ohm: I = V / R = ' + voltage.toFixed(1) + ' V / ' + totalR.toFixed(1) + ' Ω = ' + current.toFixed(2) + ' A | P = ' + power.toFixed(2) + ' W';
  }
  updateHUD();

  rangeVolt.oninput = (e) => { voltage = parseFloat(e.target.value); updateHUD(); };
  rangeRes.oninput = (e) => { resistance = parseFloat(e.target.value); updateHUD(); };
  rangeBulbs.oninput = (e) => { numBulbs = parseInt(e.target.value); updateHUD(); };

  btnSwitch.onclick = () => {
    isSwitchClosed = !isSwitchClosed;
    btnSwitch.textContent = isSwitchClosed ? 'Buka Saklar' : 'Tutup Saklar';
    btnSwitch.classList.toggle('active', isSwitchClosed);
    updateHUD();
  };

  btnReset.onclick = () => {
    voltage = 9.0;
    resistance = 10.0;
    numBulbs = 1;
    isSwitchClosed = true;
    rangeVolt.value = 9.0;
    rangeRes.value = 10.0;
    rangeBulbs.value = 1;
    btnSwitch.textContent = 'Buka Saklar';
    btnSwitch.classList.add('active');
    updateHUD();
  };

  // Draw loop
  let lastTime = performance.now();
  function draw(ts) {
    const dt = Math.min(0.05, (ts - lastTime) / 1000);
    lastTime = ts;

    const totalR = resistance * numBulbs;
    const current = isSwitchClosed ? (voltage / totalR) : 0;

    if (isSwitchClosed && current > 0) {
      electronPhase = (electronPhase + dt * current * 3.5) % 1.0;
    }

    ctx.clearRect(0, 0, width, height);

    // Subtle dot grid
    ctx.fillStyle = 'rgba(0, 0, 0, 0.04)';
    for (let x = 20; x < width; x += 24) {
      for (let y = 20; y < height; y += 24) {
        ctx.fillRect(x, y, 2, 2);
      }
    }

    // Circuit Layout Rectangle
    const padX = Math.max(70, width * 0.12);
    const padY = Math.max(50, height * 0.14);
    const cW = width - padX * 2;
    const cH = height - padY * 2 - 20;

    const leftX = padX;
    const rightX = padX + cW;
    const topY = padY;
    const bottomY = padY + cH;

    // Draw Copper Wires
    ctx.strokeStyle = '#CBD5E1';
    ctx.lineWidth = 6;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';
    ctx.beginPath();
    // Top wire
    ctx.moveTo(leftX, topY); ctx.lineTo(rightX, topY);
    // Right wire
    ctx.lineTo(rightX, bottomY);
    // Bottom wire
    ctx.lineTo(leftX, bottomY);
    // Left wire
    ctx.lineTo(leftX, topY);
    ctx.stroke();

    // Active Current Flow Wire Glow
    if (isSwitchClosed && current > 0) {
      const glowAlpha = Math.min(0.8, 0.2 + (current / 2.4) * 0.6);
      ctx.strokeStyle = 'rgba(217, 119, 6, ' + glowAlpha + ')';
      ctx.lineWidth = 4;
      ctx.stroke();
    }

    // Moving Electrons in Circuit Wire
    if (isSwitchClosed && current > 0) {
      const perimeter = 2 * (cW + cH);
      const numElectrons = 36;
      ctx.fillStyle = '#0284C7';
      for (let i = 0; i < numElectrons; i++) {
        let dist = ((i / numElectrons + electronPhase) % 1.0) * perimeter;
        let ex = leftX, ey = topY;
        if (dist < cW) { // Top wire (left -> right)
          ex = leftX + dist; ey = topY;
        } else if (dist < cW + cH) { // Right wire (top -> bottom)
          ex = rightX; ey = topY + (dist - cW);
        } else if (dist < 2 * cW + cH) { // Bottom wire (right -> left)
          ex = rightX - (dist - (cW + cH)); ey = bottomY;
        } else { // Left wire (bottom -> top)
          ex = leftX; ey = bottomY - (dist - (2 * cW + cH));
        }
        ctx.beginPath();
        ctx.arc(ex, ey, 3.5, 0, Math.PI * 2);
        ctx.fill();
      }
    }

    // 1. Draw Battery on Left Wire
    const batY = (topY + bottomY) / 2;
    const batW = 40, batH = 65;
    ctx.fillStyle = '#0F172A';
    ctx.fillRect(leftX - batW / 2, batY - batH / 2, batW, batH);
    // Battery terminal caps
    ctx.fillStyle = '#D97706';
    ctx.fillRect(leftX - 8, batY - batH / 2 - 6, 16, 6); // + top cap
    // Battery Label
    ctx.fillStyle = '#FFFFFF';
    ctx.font = 'bold 11px ui-monospace, Menlo, monospace';
    ctx.textAlign = 'center';
    ctx.fillText('+', leftX, batY - 14);
    ctx.fillText('-', leftX, batY + 22);
    ctx.fillStyle = '#0F172A';
    ctx.font = 'bold 12px ui-monospace, Menlo, monospace';
    ctx.fillText(voltage.toFixed(1) + ' V', leftX - 42, batY + 4);

    // 2. Draw Switch on Top Wire
    const swX = (leftX + rightX) / 2 - 60;
    ctx.fillStyle = '#FFFFFF';
    ctx.fillRect(swX - 25, topY - 10, 50, 20); // clear wire gap
    // Terminals
    ctx.fillStyle = '#475569';
    ctx.beginPath(); ctx.arc(swX - 18, topY, 4, 0, Math.PI * 2); ctx.fill();
    ctx.beginPath(); ctx.arc(swX + 18, topY, 4, 0, Math.PI * 2); ctx.fill();
    // Blade
    ctx.strokeStyle = '#0F172A';
    ctx.lineWidth = 3;
    ctx.beginPath();
    ctx.moveTo(swX - 18, topY);
    if (isSwitchClosed) {
      ctx.lineTo(swX + 18, topY);
    } else {
      ctx.lineTo(swX + 10, topY - 24); // open 35 deg
    }
    ctx.stroke();
    ctx.fillStyle = '#64748B';
    ctx.font = '10px ui-monospace, monospace';
    ctx.fillText(isSwitchClosed ? 'Saklar ON' : 'Saklar OFF', swX, topY - 16);

    // 3. Draw Light Bulbs on Bottom Wire
    for (let b = 0; b < numBulbs; b++) {
      const spacing = cW / (numBulbs + 1);
      const bulbX = leftX + spacing * (b + 1);
      const bulbY = bottomY;

      // Glow Aura when lit
      if (isSwitchClosed && current > 0) {
        const pNorm = Math.min(1.0, current * (voltage / totalR) / 1.5);
        const glowR = 25 + pNorm * 35;
        const grad = ctx.createRadialGradient(bulbX, bulbY, 4, bulbX, bulbY, glowR);
        grad.addColorStop(0, 'rgba(251, 191, 36, ' + (0.4 + pNorm * 0.5) + ')');
        grad.addColorStop(1, 'rgba(251, 191, 36, 0)');
        ctx.fillStyle = grad;
        ctx.beginPath();
        ctx.arc(bulbX, bulbY, glowR, 0, Math.PI * 2);
        ctx.fill();
      }

      // Bulb Glass Globe
      ctx.fillStyle = (isSwitchClosed && current > 0) ? '#FDE68A' : '#F1F5F9';
      ctx.beginPath();
      ctx.arc(bulbX, bulbY, 18, 0, Math.PI * 2);
      ctx.fill();
      ctx.strokeStyle = '#94A3B8';
      ctx.lineWidth = 2;
      ctx.stroke();

      // Filament
      ctx.strokeStyle = (isSwitchClosed && current > 0) ? '#D97706' : '#64748B';
      ctx.lineWidth = 1.5;
      ctx.beginPath();
      ctx.moveTo(bulbX - 6, bulbY + 12);
      ctx.lineTo(bulbX - 4, bulbY - 4);
      ctx.lineTo(bulbX, bulbY + 2);
      ctx.lineTo(bulbX + 4, bulbY - 4);
      ctx.lineTo(bulbX + 6, bulbY + 12);
      ctx.stroke();

      // Bulb Base Mount
      ctx.fillStyle = '#475569';
      ctx.fillRect(bulbX - 10, bulbY + 14, 20, 8);

      // Label below bulb
      ctx.fillStyle = '#0F172A';
      ctx.font = '11px ui-monospace, Menlo, monospace';
      ctx.fillText(resistance.toFixed(1) + ' Ω', bulbX, bulbY + 36);
    }

    requestAnimationFrame(draw);
  }
  requestAnimationFrame(draw);
})();
</script>
</body>
</html>
"""

    fun getSnellRefractionHtml(): String = """
<!DOCTYPE html>
<html lang="id">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; -webkit-tap-highlight-color: transparent; }
  html, body { position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; margin: 0; padding: 0; overflow: hidden; display: flex; flex-direction: column; background: #FFFFFF; color: #0F172A; user-select: none; -webkit-user-select: none; }
  #canvas-container { flex: 1 1 0%; min-height: 0; position: relative; width: 100%; background: #FFFFFF; overflow: hidden; }
  canvas { position: absolute; top: 0; left: 0; width: 100%; height: 100%; display: block; touch-action: none; }
  #glass-hud { position: absolute; top: 14px; left: 14px; background: rgba(255, 255, 255, 0.94); backdrop-filter: blur(8px); border: 1px solid #E2E8F0; border-radius: 12px; padding: 10px 14px; box-shadow: 0 4px 14px rgba(0,0,0,0.06); font-size: 12px; line-height: 1.6; max-width: 320px; pointer-events: none; z-index: 5; }
  .hud-row { display: flex; justify-content: space-between; gap: 12px; }
  .hud-val { font-weight: 700; color: #0284C7; font-family: ui-monospace, Menlo, Consolas, monospace; }
  #control-dock { flex-shrink: 0; background: #F8FAFC; border-top: 1px solid #E2E8F0; padding: 12px 18px; display: flex; flex-direction: column; gap: 10px; z-index: 10; }
  .sliders-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 12px; }
  .control-group { display: flex; flex-direction: column; gap: 4px; }
  .control-label { display: flex; justify-content: space-between; font-size: 11px; font-weight: 600; color: #64748B; }
  .val-badge { font-weight: 700; color: #0284C7; font-family: ui-monospace, Menlo, monospace; }
  input[type="range"] { width: 100%; height: 26px; accent-color: #0284C7; cursor: pointer; }
  select { width: 100%; padding: 6px 10px; font-size: 12px; font-weight: 600; border-radius: 8px; border: 1px solid #CBD5E1; background: #FFFFFF; color: #0F172A; }
  .actions-row { display: flex; align-items: center; justify-content: space-between; gap: 10px; border-top: 1px solid #E2E8F0; padding-top: 8px; }
  .btn-group { display: flex; gap: 8px; }
  button { padding: 8px 16px; font-size: 12px; font-weight: 600; border-radius: 8px; cursor: pointer; border: 1px solid #CBD5E1; background: #FFFFFF; color: #0F172A; }
  button.active { background: #0F172A; color: #FFFFFF; border-color: #0F172A; }
  .formula-box { font-family: ui-monospace, Menlo, Consolas, monospace; font-size: 11px; color: #334155; background: #FFFFFF; border: 1px solid #E2E8F0; border-radius: 6px; padding: 6px 12px; }

  /* ── Studio Workbench Layout Upgrade ── */
  html, body {
    position: fixed !important;
    inset: 0 !important;
    width: 100vw !important;
    height: 100vh !important;
    margin: 0 !important;
    padding: 0 !important;
    overflow: hidden !important;
    display: flex !important;
    flex-direction: row !important;
    background: #FFFFFF !important;
    color: #0F172A !important;
    user-select: none !important;
    -webkit-user-select: none !important;
  }
  #canvas-container, #main-container {
    flex: 1 1 0% !important;
    min-width: 0 !important;
    height: 100% !important;
    position: relative !important;
    background: #FFFFFF !important;
    overflow: hidden !important;
  }
  #control-dock, #inspector-sidebar {
    width: 340px !important;
    max-width: 35vw !important;
    flex-shrink: 0 !important;
    height: 100% !important;
    background: #F8FAFC !important;
    border-left: 1px solid #E2E8F0 !important;
    border-top: none !important;
    padding: 20px 18px !important;
    display: flex !important;
    flex-direction: column !important;
    gap: 14px !important;
    overflow-y: auto !important;
    z-index: 10 !important;
    box-shadow: -4px 0 16px rgba(0,0,0,0.02) !important;
  }
  .sliders-grid {
    display: flex !important;
    flex-direction: column !important;
    gap: 12px !important;
  }
  .control-group {
    display: flex !important;
    flex-direction: column !important;
    gap: 6px !important;
    background: #FFFFFF !important;
    border: 1px solid #E2E8F0 !important;
    border-radius: 10px !important;
    padding: 10px 12px !important;
    box-shadow: 0 1px 3px rgba(0,0,0,0.02) !important;
  }
  .actions-row {
    display: flex !important;
    flex-direction: column !important;
    gap: 10px !important;
    border-top: 1px solid #E2E8F0 !important;
    padding-top: 14px !important;
    margin-top: auto !important;
  }
  .btn-group {
    display: flex !important;
    gap: 8px !important;
    width: 100% !important;
  }
  .btn-group button {
    flex: 1 !important;
  }
  .formula-box, .theory-card {
    font-family: ui-monospace, Menlo, Consolas, monospace !important;
    font-size: 11px !important;
    color: #0369A1 !important;
    background: #EFF6FF !important;
    border: 1px solid #BFDBFE !important;
    border-radius: 8px !important;
    padding: 10px 12px !important;
    line-height: 1.5 !important;
    word-break: break-word !important;
  }

</style>
</head>
<body>
<div id="canvas-container">
  <canvas id="simCanvas"></canvas>
  <div id="glass-hud">
    <div class="hud-row"><span>Status:</span><span id="hudStatus" class="hud-val">Pembiasan Normal</span></div>
    <div class="hud-row"><span>Sudut Datang (θ₁):</span><span id="hudTheta1" class="hud-val">30.0°</span></div>
    <div class="hud-row"><span>Sudut Bias (θ₂):</span><span id="hudTheta2" class="hud-val">19.5°</span></div>
    <div class="hud-row"><span>Sudut Kritis (θc):</span><span id="hudThetaC" class="hud-val">Tidak Ada</span></div>
    <div class="hud-row"><span>Kecepatan Cahaya v₂:</span><span id="hudSpeed2" class="hud-val">2.00 × 10⁸ m/s</span></div>
  </div>
</div>
<div id="control-dock">
  <div class="sliders-grid">
    <div class="control-group">
      <div class="control-label"><span>Sudut Datang (θ₁)</span><span class="val-badge" id="valAngle">30.0°</span></div>
      <input type="range" id="rangeAngle" min="0" max="85" step="0.5" value="30">
    </div>
    <div class="control-group">
      <div class="control-label"><span>Medium 1 (Atas)</span><span class="val-badge" id="valN1">n₁ = 1.00</span></div>
      <select id="selectM1">
        <option value="1.000" selected>Udara (n = 1.00)</option>
        <option value="1.333">Air (n = 1.33)</option>
        <option value="1.500">Kaca Mahkota (n = 1.50)</option>
        <option value="2.417">Intan Berlian (n = 2.42)</option>
      </select>
    </div>
    <div class="control-group">
      <div class="control-label"><span>Medium 2 (Bawah)</span><span class="val-badge" id="valN2">n₂ = 1.50</span></div>
      <select id="selectM2">
        <option value="1.000">Udara (n = 1.00)</option>
        <option value="1.333">Air (n = 1.33)</option>
        <option value="1.500" selected>Kaca Mahkota (n = 1.50)</option>
        <option value="2.417">Intan Berlian (n = 2.42)</option>
      </select>
    </div>
    <div class="control-group">
      <div class="control-label"><span>Alat Bantu Visual</span><span class="val-badge" id="valTool">Busur Aktif</span></div>
      <div class="btn-group">
        <button id="btnProtractor" class="active" style="flex:1;">Busur Derajat</button>
      </div>
    </div>
  </div>
  <div class="actions-row">
    <div class="btn-group">
      <button id="btnSwap">Tukar Medium</button>
      <button id="btnReset">Reset Default</button>
    </div>
    <div class="formula-box" id="formulaPill">
      Hukum Snellius: n₁ · sin θ₁ = n₂ · sin θ₂
    </div>
  </div>
</div>

<script>
(function() {
  const container = document.getElementById('canvas-container');
  const canvas = document.getElementById('simCanvas');
  const ctx = canvas.getContext('2d');

  let width = 600, height = 400;
  function resize() {
    const w = container.clientWidth || 600;
    const h = container.clientHeight || 400;
    const dpr = window.devicePixelRatio || 1;
    canvas.width = Math.round(w * dpr);
    canvas.height = Math.round(h * dpr);
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    width = w;
    height = h;
  }
  window.addEventListener('resize', resize);
  if (window.ResizeObserver) {
    new ResizeObserver(resize).observe(container);
  }
  resize();

  // Params
  let theta1Deg = 30.0;
  let n1 = 1.000;
  let n2 = 1.500;
  let showProtractor = true;
  let isDraggingLaser = false;

  // DOM elements
  const rangeAngle = document.getElementById('rangeAngle');
  const selectM1 = document.getElementById('selectM1');
  const selectM2 = document.getElementById('selectM2');
  const valAngle = document.getElementById('valAngle');
  const valN1 = document.getElementById('valN1');
  const valN2 = document.getElementById('valN2');
  const valTool = document.getElementById('valTool');
  const hudStatus = document.getElementById('hudStatus');
  const hudTheta1 = document.getElementById('hudTheta1');
  const hudTheta2 = document.getElementById('hudTheta2');
  const hudThetaC = document.getElementById('hudThetaC');
  const hudSpeed2 = document.getElementById('hudSpeed2');
  const formulaPill = document.getElementById('formulaPill');
  const btnProtractor = document.getElementById('btnProtractor');
  const btnSwap = document.getElementById('btnSwap');
  const btnReset = document.getElementById('btnReset');

  function getRefraction() {
    const t1Rad = theta1Deg * Math.PI / 180;
    const sinT2 = (n1 / n2) * Math.sin(t1Rad);
    let isTIR = false;
    let t2Deg = 0;
    if (Math.abs(sinT2) > 1.0) {
      isTIR = true;
    } else {
      t2Deg = Math.asin(sinT2) * 180 / Math.PI;
    }

    let critAngle = null;
    if (n1 > n2) {
      critAngle = Math.asin(n2 / n1) * 180 / Math.PI;
    }
    return { isTIR: isTIR, t2Deg: t2Deg, critAngle: critAngle, sinT2: sinT2 };
  }

  function updateHUD() {
    valAngle.textContent = theta1Deg.toFixed(1) + '°';
    valN1.textContent = 'n₁ = ' + n1.toFixed(2);
    valN2.textContent = 'n₂ = ' + n2.toFixed(2);
    valTool.textContent = showProtractor ? 'Busur Aktif' : 'Busur Sembunyi';

    const res = getRefraction();
    hudTheta1.textContent = theta1Deg.toFixed(1) + '°';

    if (res.isTIR) {
      hudStatus.textContent = 'Pemantulan Sempurna (TIR)';
      hudStatus.style.color = '#DC2626';
      hudTheta2.textContent = 'Tidak Ada (TIR)';
      hudTheta2.style.color = '#DC2626';
    } else {
      hudStatus.textContent = 'Pembiasan Cahaya';
      hudStatus.style.color = '#0284C7';
      hudTheta2.textContent = res.t2Deg.toFixed(1) + '°';
      hudTheta2.style.color = '#0284C7';
    }

    if (res.critAngle !== null) {
      hudThetaC.textContent = res.critAngle.toFixed(1) + '°';
      if (theta1Deg >= res.critAngle) {
        hudThetaC.style.color = '#DC2626';
      } else {
        hudThetaC.style.color = '#D97706';
      }
    } else {
      hudThetaC.textContent = 'Tidak Ada (n₁ ≤ n₂)';
      hudThetaC.style.color = '#64748B';
    }

    const v2 = (3.0 / n2).toFixed(2);
    hudSpeed2.textContent = v2 + ' × 10⁸ m/s';

    if (res.isTIR) {
      formulaPill.textContent = 'TIR: θ₁ (' + theta1Deg.toFixed(1) + '°) ≥ θc (' + (res.critAngle ? res.critAngle.toFixed(1) : '') + '°) | 100% Sinar Dipantulkan';
    } else {
      formulaPill.textContent = 'Hukum Snellius: ' + n1.toFixed(2) + ' · sin(' + theta1Deg.toFixed(1) + '°) = ' + n2.toFixed(2) + ' · sin(' + res.t2Deg.toFixed(1) + '°)';
    }
  }
  updateHUD();

  rangeAngle.oninput = (e) => {
    theta1Deg = parseFloat(e.target.value);
    updateHUD();
  };
  selectM1.onchange = (e) => {
    n1 = parseFloat(e.target.value);
    updateHUD();
  };
  selectM2.onchange = (e) => {
    n2 = parseFloat(e.target.value);
    updateHUD();
  };
  btnProtractor.onclick = () => {
    showProtractor = !showProtractor;
    btnProtractor.classList.toggle('active', showProtractor);
    updateHUD();
  };
  btnSwap.onclick = () => {
    const temp = n1;
    n1 = n2;
    n2 = temp;
    selectM1.value = n1.toFixed(3);
    selectM2.value = n2.toFixed(3);
    updateHUD();
  };
  btnReset.onclick = () => {
    theta1Deg = 30.0;
    n1 = 1.000;
    n2 = 1.500;
    rangeAngle.value = 30.0;
    selectM1.value = '1.000';
    selectM2.value = '1.500';
    showProtractor = true;
    btnProtractor.classList.add('active');
    updateHUD();
  };

  // Direct pointer drag for laser pointer
  canvas.addEventListener('pointerdown', (e) => {
    const rect = canvas.getBoundingClientRect();
    const mx = e.clientX - rect.left;
    const my = e.clientY - rect.top;
    const cx = width / 2;
    const cy = height / 2;

    // Check if clicked in upper-left quadrant
    if (my < cy && mx < cx) {
      isDraggingLaser = true;
      calcAngleFromMouse(mx, my);
    }
  });

  window.addEventListener('pointermove', (e) => {
    if (!isDraggingLaser) return;
    const rect = canvas.getBoundingClientRect();
    const mx = e.clientX - rect.left;
    const my = e.clientY - rect.top;
    calcAngleFromMouse(mx, my);
  });

  window.addEventListener('pointerup', () => { isDraggingLaser = false; });

  function calcAngleFromMouse(mx, my) {
    const cx = width / 2;
    const cy = height / 2;
    const dx = cx - mx;
    const dy = cy - my;
    if (dy <= 5) return;
    let deg = Math.atan2(dx, dy) * 180 / Math.PI;
    deg = Math.max(0, Math.min(85, deg));
    theta1Deg = Math.round(deg * 10) / 10;
    rangeAngle.value = theta1Deg;
    updateHUD();
  }

  function draw() {
    ctx.clearRect(0, 0, width, height);

    const cx = width / 2;
    const cy = height / 2;

    // Background Medium 1 (Top)
    const m1Alpha = Math.min(0.25, 0.04 + (n1 - 1.0) * 0.15);
    ctx.fillStyle = 'rgba(2, 132, 199, ' + m1Alpha + ')';
    ctx.fillRect(0, 0, width, cy);

    // Background Medium 2 (Bottom)
    const m2Alpha = Math.min(0.35, 0.04 + (n2 - 1.0) * 0.15);
    ctx.fillStyle = 'rgba(16, 185, 129, ' + m2Alpha + ')';
    ctx.fillRect(0, cy, width, height - cy);

    // Medium labels
    ctx.font = 'bold 13px -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif';
    ctx.fillStyle = '#0369A1';
    ctx.fillText('Medium 1: ' + selectM1.options[selectM1.selectedIndex].text, 20, cy - 20);
    ctx.fillStyle = '#047857';
    ctx.fillText('Medium 2: ' + selectM2.options[selectM2.selectedIndex].text, 20, cy + 30);

    // Boundary interface line
    ctx.strokeStyle = '#0F172A';
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.moveTo(0, cy);
    ctx.lineTo(width, cy);
    ctx.stroke();

    // Normal line (dashed)
    ctx.strokeStyle = '#94A3B8';
    ctx.lineWidth = 1.5;
    ctx.setLineDash([5, 5]);
    ctx.beginPath();
    ctx.moveTo(cx, 20);
    ctx.lineTo(cx, height - 20);
    ctx.stroke();
    ctx.setLineDash([]);

    // Normal label
    ctx.fillStyle = '#64748B';
    ctx.font = '10px ui-monospace, Menlo, monospace';
    ctx.fillText('Garis Normal', cx + 6, 32);

    // Protractor (if enabled)
    if (showProtractor) {
      const protR = Math.min(130, Math.min(cx, cy) * 0.65);
      ctx.strokeStyle = 'rgba(100, 116, 139, 0.25)';
      ctx.lineWidth = 1;
      ctx.beginPath();
      ctx.arc(cx, cy, protR, 0, Math.PI * 2);
      ctx.stroke();

      // Degree tick marks
      ctx.font = '9px ui-monospace, Menlo, monospace';
      ctx.fillStyle = '#94A3B8';
      for (let a = 0; a < 360; a += 15) {
        const rad = a * Math.PI / 180;
        const tickLen = (a % 30 === 0) ? 8 : 4;
        const x1 = cx + Math.cos(rad) * (protR - tickLen);
        const y1 = cy + Math.sin(rad) * (protR - tickLen);
        const x2 = cx + Math.cos(rad) * protR;
        const y2 = cy + Math.sin(rad) * protR;
        ctx.beginPath();
        ctx.moveTo(x1, y1);
        ctx.lineTo(x2, y2);
        ctx.stroke();
      }
    }

    const t1Rad = theta1Deg * Math.PI / 180;
    const rayLen = Math.min(width, height) * 0.42;
    const res = getRefraction();

    // 1. Incident Ray (Yellow Laser)
    const ix = cx - Math.sin(t1Rad) * rayLen;
    const iy = cy - Math.cos(t1Rad) * rayLen;

    // Laser Pointer body
    const laserAngle = Math.atan2(cy - iy, cx - ix);
    ctx.save();
    ctx.translate(ix, iy);
    ctx.rotate(laserAngle);
    ctx.fillStyle = '#334155';
    ctx.fillRect(-35, -9, 35, 18);
    ctx.fillStyle = '#D97706';
    ctx.fillRect(0, -5, 6, 10);
    ctx.fillStyle = '#94A3B8';
    ctx.fillRect(-32, -6, 8, 12);
    ctx.restore();

    // Incident Beam
    ctx.strokeStyle = '#D97706';
    ctx.lineWidth = 3.5;
    ctx.beginPath();
    ctx.moveTo(ix, iy);
    ctx.lineTo(cx, cy);
    ctx.stroke();

    // Incident Ray Glow
    ctx.strokeStyle = 'rgba(245, 158, 11, 0.25)';
    ctx.lineWidth = 8;
    ctx.beginPath();
    ctx.moveTo(ix, iy);
    ctx.lineTo(cx, cy);
    ctx.stroke();

    // Incident arrow
    const midIx = (ix + cx) / 2;
    const midIy = (iy + cy) / 2;
    drawArrow(midIx, midIy, laserAngle, '#D97706');

    // Angle theta1 Arc
    if (theta1Deg > 1) {
      const arcR = 45;
      ctx.strokeStyle = '#D97706';
      ctx.lineWidth = 2;
      ctx.beginPath();
      ctx.arc(cx, cy, arcR, -Math.PI / 2 - t1Rad, -Math.PI / 2);
      ctx.stroke();

      ctx.fillStyle = '#D97706';
      ctx.font = 'bold 11px ui-monospace, Menlo, monospace';
      const labelA = -Math.PI / 2 - t1Rad / 2;
      ctx.fillText(theta1Deg.toFixed(1) + '°', cx + Math.cos(labelA) * 60 - 15, cy + Math.sin(labelA) * 60);
    }

    // 2. Reflected Ray (Always exists, bold during TIR)
    const rx = cx + Math.sin(t1Rad) * rayLen;
    const ry = cy - Math.cos(t1Rad) * rayLen;
    const reflAngle = Math.atan2(ry - cy, rx - cx);

    if (res.isTIR) {
      ctx.strokeStyle = '#DC2626';
      ctx.lineWidth = 3.5;
      ctx.beginPath();
      ctx.moveTo(cx, cy);
      ctx.lineTo(rx, ry);
      ctx.stroke();

      ctx.strokeStyle = 'rgba(220, 38, 38, 0.3)';
      ctx.lineWidth = 9;
      ctx.beginPath();
      ctx.moveTo(cx, cy);
      ctx.lineTo(rx, ry);
      ctx.stroke();

      drawArrow((cx + rx) / 2, (cy + ry) / 2, reflAngle, '#DC2626');
    } else {
      ctx.strokeStyle = 'rgba(217, 119, 6, 0.45)';
      ctx.lineWidth = 2;
      ctx.beginPath();
      ctx.moveTo(cx, cy);
      ctx.lineTo(rx, ry);
      ctx.stroke();

      drawArrow((cx + rx) / 2, (cy + ry) / 2, reflAngle, 'rgba(217, 119, 6, 0.5)');
    }

    // 3. Refracted Ray (Green, only if not TIR)
    if (!res.isTIR) {
      const t2Rad = res.t2Deg * Math.PI / 180;
      const bx = cx + Math.sin(t2Rad) * rayLen;
      const by = cy + Math.cos(t2Rad) * rayLen;
      const refrAngle = Math.atan2(by - cy, bx - cx);

      ctx.strokeStyle = '#059669';
      ctx.lineWidth = 3.5;
      ctx.beginPath();
      ctx.moveTo(cx, cy);
      ctx.lineTo(bx, by);
      ctx.stroke();

      ctx.strokeStyle = 'rgba(5, 150, 105, 0.25)';
      ctx.lineWidth = 8;
      ctx.beginPath();
      ctx.moveTo(cx, cy);
      ctx.lineTo(bx, by);
      ctx.stroke();

      drawArrow((cx + bx) / 2, (cy + by) / 2, refrAngle, '#059669');

      // Angle theta2 Arc
      if (res.t2Deg > 1) {
        const arcR2 = 45;
        ctx.strokeStyle = '#059669';
        ctx.lineWidth = 2;
        ctx.beginPath();
        ctx.arc(cx, cy, arcR2, Math.PI / 2 - t2Rad, Math.PI / 2);
        ctx.stroke();

        ctx.fillStyle = '#059669';
        ctx.font = 'bold 11px ui-monospace, Menlo, monospace';
        const labelA2 = Math.PI / 2 - t2Rad / 2;
        ctx.fillText(res.t2Deg.toFixed(1) + '°', cx + Math.cos(labelA2) * 60 + 5, cy + Math.sin(labelA2) * 60 + 5);
      }
    } else {
      // Draw TIR Warning Box in lower quadrant
      ctx.fillStyle = 'rgba(254, 226, 226, 0.9)';
      ctx.strokeStyle = '#F87171';
      ctx.lineWidth = 1;
      const badgeW = 220;
      ctx.fillRect(cx - badgeW / 2, cy + 40, badgeW, 40);
      ctx.strokeRect(cx - badgeW / 2, cy + 40, badgeW, 40);

      ctx.fillStyle = '#B91C1C';
      ctx.font = 'bold 11px -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText('PEMANTULAN SEMPURNA', cx, cy + 56);
      ctx.font = '10px ui-monospace, Menlo, monospace';
      ctx.fillText('Tidak ada sinar yang dibiaskan ke Medium 2', cx, cy + 72);
      ctx.textAlign = 'left';
    }

    // Hit point flash
    ctx.fillStyle = '#FFFFFF';
    ctx.beginPath();
    ctx.arc(cx, cy, 4, 0, Math.PI * 2);
    ctx.fill();
    ctx.strokeStyle = '#0F172A';
    ctx.lineWidth = 1.5;
    ctx.stroke();

    requestAnimationFrame(draw);
  }

  function drawArrow(x, y, angle, color) {
    ctx.save();
    ctx.translate(x, y);
    ctx.rotate(angle);
    ctx.fillStyle = color;
    ctx.beginPath();
    ctx.moveTo(8, 0);
    ctx.lineTo(-6, -5);
    ctx.lineTo(-3, 0);
    ctx.lineTo(-6, 5);
    ctx.closePath();
    ctx.fill();
    ctx.restore();
  }

  draw();
})();
</script>
</body>
</html>
"""

    fun getFrictionSimulationHtml(): String = """
﻿<!DOCTYPE html>
<html lang="id">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; -webkit-tap-highlight-color: transparent; }
  html, body { width: 100%; height: 100%; overflow: hidden; display: flex; flex-direction: column; background: #FFFFFF; color: #0F172A; user-select: none; -webkit-user-select: none; }
  #canvas-container { flex: 1 1 0%; min-height: 0; position: relative; width: 100%; background: #FFFFFF; overflow: hidden; }
  canvas { position: absolute; top: 0; left: 0; width: 100%; height: 100%; display: block; touch-action: none; }
  #glass-hud { position: absolute; top: 14px; left: 14px; background: rgba(255, 255, 255, 0.94); backdrop-filter: blur(8px); border: 1px solid #E2E8F0; border-radius: 12px; padding: 10px 14px; box-shadow: 0 4px 14px rgba(0,0,0,0.06); font-size: 12px; line-height: 1.6; max-width: 320px; pointer-events: none; z-index: 5; }
  .hud-row { display: flex; justify-content: space-between; gap: 12px; }
  .hud-val { font-weight: 700; color: #0284C7; font-family: ui-monospace, Menlo, Consolas, monospace; }
  #control-dock { flex-shrink: 0; background: #F8FAFC; border-top: 1px solid #E2E8F0; padding: 12px 18px; display: flex; flex-direction: column; gap: 10px; z-index: 10; }
  .sliders-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap: 12px; }
  .control-group { display: flex; flex-direction: column; gap: 4px; }
  .control-label { display: flex; justify-content: space-between; font-size: 11px; font-weight: 600; color: #64748B; }
  .val-badge { font-weight: 700; color: #0284C7; font-family: ui-monospace, Menlo, monospace; }
  input[type="range"] { width: 100%; height: 26px; accent-color: #0284C7; cursor: pointer; }
  .actions-row { display: flex; align-items: center; justify-content: space-between; gap: 10px; border-top: 1px solid #E2E8F0; padding-top: 8px; }
  .btn-group { display: flex; gap: 8px; }
  button { padding: 8px 16px; font-size: 12px; font-weight: 600; border-radius: 8px; cursor: pointer; border: 1px solid #CBD5E1; background: #FFFFFF; color: #0F172A; }
  button.active { background: #0F172A; color: #FFFFFF; border-color: #0F172A; }
  .formula-box { font-family: ui-monospace, Menlo, Consolas, monospace; font-size: 11px; color: #334155; background: #FFFFFF; border: 1px solid #E2E8F0; border-radius: 6px; padding: 6px 12px; }

  /* ── Studio Workbench Layout Upgrade ── */
  html, body {
    position: fixed !important;
    inset: 0 !important;
    width: 100vw !important;
    height: 100vh !important;
    margin: 0 !important;
    padding: 0 !important;
    overflow: hidden !important;
    display: flex !important;
    flex-direction: row !important;
    background: #FFFFFF !important;
    color: #0F172A !important;
    user-select: none !important;
    -webkit-user-select: none !important;
  }
  #canvas-container, #main-container {
    flex: 1 1 0% !important;
    min-width: 0 !important;
    height: 100% !important;
    position: relative !important;
    background: #FFFFFF !important;
    overflow: hidden !important;
  }
  #control-dock, #inspector-sidebar {
    width: 340px !important;
    max-width: 35vw !important;
    flex-shrink: 0 !important;
    height: 100% !important;
    background: #F8FAFC !important;
    border-left: 1px solid #E2E8F0 !important;
    border-top: none !important;
    padding: 20px 18px !important;
    display: flex !important;
    flex-direction: column !important;
    gap: 14px !important;
    overflow-y: auto !important;
    z-index: 10 !important;
    box-shadow: -4px 0 16px rgba(0,0,0,0.02) !important;
  }
  .sliders-grid {
    display: flex !important;
    flex-direction: column !important;
    gap: 12px !important;
  }
  .control-group {
    display: flex !important;
    flex-direction: column !important;
    gap: 6px !important;
    background: #FFFFFF !important;
    border: 1px solid #E2E8F0 !important;
    border-radius: 10px !important;
    padding: 10px 12px !important;
    box-shadow: 0 1px 3px rgba(0,0,0,0.02) !important;
  }
  .actions-row {
    display: flex !important;
    flex-direction: column !important;
    gap: 10px !important;
    border-top: 1px solid #E2E8F0 !important;
    padding-top: 14px !important;
    margin-top: auto !important;
  }
  .btn-group {
    display: flex !important;
    gap: 8px !important;
    width: 100% !important;
  }
  .btn-group button {
    flex: 1 !important;
  }
  .formula-box, .theory-card {
    font-family: ui-monospace, Menlo, Consolas, monospace !important;
    font-size: 11px !important;
    color: #0369A1 !important;
    background: #EFF6FF !important;
    border: 1px solid #BFDBFE !important;
    border-radius: 8px !important;
    padding: 10px 12px !important;
    line-height: 1.5 !important;
    word-break: break-word !important;
  }

</style>
</head>
<body>
<div id="canvas-container">
  <canvas id="simCanvas"></canvas>
  <div id="glass-hud">
    <div class="hud-row"><span>Status:</span><span id="hudStatus" class="hud-val">Ekuilibrium (Diam)</span></div>
    <div class="hud-row"><span>Komponen Berat (W‖):</span><span id="hudWpar" class="hud-val">24.5 N</span></div>
    <div class="hud-row"><span>Gaya Gesek (f):</span><span id="hudFriction" class="hud-val">24.5 N</span></div>
    <div class="hud-row"><span>Percepatan (a):</span><span id="hudAccel" class="hud-val">0.00 m/s²</span></div>
  </div>
</div>
<div id="control-dock">
  <div class="sliders-grid">
    <div class="control-group">
      <div class="control-label"><span>Sudut Kemiringan (θ)</span><span class="val-badge" id="valAngle">30.0°</span></div>
      <input type="range" id="rangeAngle" min="0" max="65" step="0.5" value="30">
    </div>
    <div class="control-group">
      <div class="control-label"><span>Koef. Gesek Statis (μs)</span><span class="val-badge" id="valMus">0.50</span></div>
      <input type="range" id="rangeMus" min="0" max="1.2" step="0.05" value="0.50">
    </div>
    <div class="control-group">
      <div class="control-label"><span>Koef. Gesek Kinetis (μk)</span><span class="val-badge" id="valMuk">0.30</span></div>
      <input type="range" id="rangeMuk" min="0" max="1.2" step="0.05" value="0.30">
    </div>
    <div class="control-group">
      <div class="control-label"><span>Massa Benda (m)</span><span class="val-badge" id="valMass">5.0 kg</span></div>
      <input type="range" id="rangeMass" min="1" max="25" step="0.5" value="5">
    </div>
  </div>
  <div class="actions-row">
    <div class="btn-group">
      <button id="btnPlay" class="active">Jeda</button>
      <button id="btnReset">Reset Balok</button>
    </div>
    <div class="formula-box" id="formulaPill">
      a = g · (sin θ - μk · cos θ) = 0.00 m/s²
    </div>
  </div>
</div>

<script>
(function() {
  const container = document.getElementById('canvas-container');
  const canvas = document.getElementById('simCanvas');
  const ctx = canvas.getContext('2d');

  let width = 600, height = 400;
  function resize() {
    const w = container.clientWidth || 600;
    const h = container.clientHeight || 400;
    const dpr = window.devicePixelRatio || 1;
    canvas.width = Math.round(w * dpr);
    canvas.height = Math.round(h * dpr);
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    width = w;
    height = h;
  }
  window.addEventListener('resize', resize);
  if (window.ResizeObserver) {
    new ResizeObserver(resize).observe(container);
  }
  resize();

  // Params
  let thetaDeg = 30;
  let mus = 0.50;
  let muk = 0.30;
  let mass = 5.0;
  const g = 9.81;

  let isPlaying = true;
  let s = 0.15; // normalized position along ramp (0 = top, 1 = bottom)
  let velocity = 0;
  let lastTime = performance.now();

  // UI
  const rangeAngle = document.getElementById('rangeAngle');
  const rangeMus = document.getElementById('rangeMus');
  const rangeMuk = document.getElementById('rangeMuk');
  const rangeMass = document.getElementById('rangeMass');
  const valAngle = document.getElementById('valAngle');
  const valMus = document.getElementById('valMus');
  const valMuk = document.getElementById('valMuk');
  const valMass = document.getElementById('valMass');
  const hudStatus = document.getElementById('hudStatus');
  const hudWpar = document.getElementById('hudWpar');
  const hudFriction = document.getElementById('hudFriction');
  const hudAccel = document.getElementById('hudAccel');
  const formulaPill = document.getElementById('formulaPill');
  const btnPlay = document.getElementById('btnPlay');
  const btnReset = document.getElementById('btnReset');

  function resetBlock() {
    s = 0.15;
    velocity = 0;
  }

  rangeAngle.oninput = (e) => {
    thetaDeg = parseFloat(e.target.value);
    valAngle.textContent = thetaDeg.toFixed(1) + '°';
    resetBlock();
  };
  rangeMus.oninput = (e) => {
    mus = parseFloat(e.target.value);
    if (muk > mus) { muk = mus; rangeMuk.value = mus; valMuk.textContent = muk.toFixed(2); }
    valMus.textContent = mus.toFixed(2);
    resetBlock();
  };
  rangeMuk.oninput = (e) => {
    muk = parseFloat(e.target.value);
    if (muk > mus) { mus = muk; rangeMus.value = muk; valMus.textContent = mus.toFixed(2); }
    valMuk.textContent = muk.toFixed(2);
    resetBlock();
  };
  rangeMass.oninput = (e) => {
    mass = parseFloat(e.target.value);
    valMass.textContent = mass.toFixed(1) + ' kg';
    resetBlock();
  };

  btnPlay.onclick = () => {
    isPlaying = !isPlaying;
    btnPlay.textContent = isPlaying ? 'Jeda' : 'Lanjutkan';
    btnPlay.classList.toggle('active', isPlaying);
  };
  btnReset.onclick = resetBlock;

  // Touch / Mouse Dragging for Ramp Angle (Reverse-engineered from all-science-sims!)
  let isDraggingRamp = false;
  function getPos(e) {
    const rect = canvas.getBoundingClientRect();
    const clientX = e.touches ? e.touches[0].clientX : e.clientX;
    const clientY = e.touches ? e.touches[0].clientY : e.clientY;
    return { x: clientX - rect.left, y: clientY - rect.top };
  }

  canvas.addEventListener('pointerdown', (e) => {
    const p = getPos(e);
    const geom = getRampGeom();
    if (Math.hypot(p.x - geom.topX, p.y - geom.topY) < 35) {
      isDraggingRamp = true;
      canvas.setPointerCapture(e.pointerId);
    }
  });

  canvas.addEventListener('pointermove', (e) => {
    if (!isDraggingRamp) return;
    const p = getPos(e);
    const geom = getRampGeom();
    const dx = p.x - geom.startX;
    const dy = geom.groundY - p.y;
    if (dx > 20 && dy > 0) {
      const angle = Math.atan2(dy, dx) * 180 / Math.PI;
      thetaDeg = Math.max(0, Math.min(65, angle));
      rangeAngle.value = thetaDeg;
      valAngle.textContent = thetaDeg.toFixed(1) + '°';
      resetBlock();
    }
  });

  canvas.addEventListener('pointerup', () => { isDraggingRamp = false; });
  canvas.addEventListener('pointercancel', () => { isDraggingRamp = false; });

  function getRampGeom() {
    const groundY = height - 50;
    const startX = Math.max(40, width * 0.12);
    const rampLen = Math.min(width * 0.72, 600);
    const rad = thetaDeg * Math.PI / 180;
    const dx = rampLen * Math.cos(rad);
    const dy = rampLen * Math.sin(rad);
    const topX = startX + dx;
    const topY = groundY - dy;
    return { startX, groundY, topX, topY, rampLen, rad };
  }

  function drawArrow(fromX, fromY, toX, toY, color, label) {
    const dx = toX - fromX;
    const dy = toY - fromY;
    const len = Math.hypot(dx, dy);
    if (len < 4) return;
    const angle = Math.atan2(dy, dx);
    ctx.strokeStyle = color;
    ctx.fillStyle = color;
    ctx.lineWidth = 2.5;

    ctx.beginPath();
    ctx.moveTo(fromX, fromY);
    ctx.lineTo(toX, toY);
    ctx.stroke();

    const head = 9;
    ctx.beginPath();
    ctx.moveTo(toX, toY);
    ctx.lineTo(toX - head * Math.cos(angle - Math.PI / 6), toY - head * Math.sin(angle - Math.PI / 6));
    ctx.lineTo(toX - head * Math.cos(angle + Math.PI / 6), toY - head * Math.sin(angle + Math.PI / 6));
    ctx.closePath();
    ctx.fill();

    if (label) {
      ctx.font = 'bold 11px -apple-system, sans-serif';
      ctx.fillStyle = '#0F172A';
      ctx.fillText(label, toX + 6, toY + 4);
    }
  }

  function step(now) {
    const dt = Math.min(0.05, (now - lastTime) / 1000);
    lastTime = now;

    const geom = getRampGeom();
    const rad = geom.rad;
    const cosTheta = Math.cos(rad);
    const sinTheta = Math.sin(rad);

    const W = mass * g;
    const Wpar = W * sinTheta;
    const Wperp = W * cosTheta;
    const maxFs = mus * Wperp;

    let accel = 0;
    let friction = 0;
    let statusText = "Ekuilibrium (Diam)";

    if (velocity > 0.001 || Wpar > maxFs) {
      friction = muk * Wperp;
      accel = (Wpar - friction) / mass;
      if (accel < 0 && velocity <= 0.001) { accel = 0; velocity = 0; }
      statusText = "Meluncur Dipercepat";
    } else {
      friction = Wpar;
      accel = 0;
      velocity = 0;
      statusText = "Ekuilibrium (Diam)";
    }

    if (isPlaying) {
      if (s < 0.92) {
        velocity += accel * dt;
        s += (velocity * dt * 0.12);
        if (s >= 0.92) { s = 0.92; velocity = 0; statusText = "Berhenti di Bawah"; }
      }
    }

    // Update HUD
    hudStatus.textContent = statusText;
    hudWpar.textContent = Wpar.toFixed(1) + ' N';
    hudFriction.textContent = friction.toFixed(1) + ' N';
    hudAccel.textContent = accel.toFixed(2) + ' m/s²';
    formulaPill.textContent = 'a = g · (sin(' + thetaDeg.toFixed(1) + '°) - ' + muk + ' · cos(' + thetaDeg.toFixed(1) + '°)) = ' + accel.toFixed(2) + ' m/s²';

    // Clear with Clean White
    ctx.clearRect(0, 0, width, height);
    ctx.fillStyle = '#FFFFFF';
    ctx.fillRect(0, 0, width, height);

    // Subtle Dot Grid
    ctx.fillStyle = 'rgba(0, 0, 0, 0.04)';
    for (let x = 0; x < width; x += 24) {
      for (let y = 0; y < height; y += 24) {
        ctx.fillRect(x, y, 1.5, 1.5);
      }
    }

    // Ground Line
    ctx.fillStyle = '#E2E8F0';
    ctx.fillRect(0, geom.groundY, width, 4);

    // Incline Ramp
    ctx.beginPath();
    ctx.moveTo(geom.startX, geom.groundY);
    ctx.lineTo(geom.topX, geom.topY);
    ctx.lineTo(geom.topX, geom.groundY);
    ctx.closePath();
    ctx.fillStyle = '#F8FAFC';
    ctx.fill();
    ctx.strokeStyle = '#CBD5E1';
    ctx.lineWidth = 2.5;
    ctx.stroke();

    // Angle Arc
    ctx.beginPath();
    ctx.arc(geom.topX, geom.groundY, 32, Math.PI, Math.PI + rad);
    ctx.strokeStyle = '#7C3AED';
    ctx.lineWidth = 2;
    ctx.stroke();
    ctx.font = 'bold 12px -apple-system, sans-serif';
    ctx.fillStyle = '#7C3AED';
    ctx.fillText('θ = ' + thetaDeg.toFixed(1) + '°', geom.startX + 14, geom.groundY - 14);

    // Interactive Drag Handle Indicator at Ramp Top
    ctx.beginPath();
    ctx.arc(geom.topX, geom.topY, 14, 0, Math.PI * 2);
    ctx.fillStyle = isDraggingRamp ? '#0284C7' : 'rgba(2, 132, 199, 0.2)';
    ctx.fill();
    ctx.strokeStyle = '#0284C7';
    ctx.lineWidth = 2;
    ctx.stroke();

    // Block
    const blockSz = 40 + mass * 0.8;
    // Position from top down towards bottom-left
    const blockX = geom.topX - s * (geom.topX - geom.startX);
    const blockY = geom.topY + s * (geom.groundY - geom.topY);

    ctx.save();
    ctx.translate(blockX, blockY);
    ctx.rotate(rad);

    ctx.fillStyle = '#0284C7';
    ctx.fillRect(-blockSz / 2, -blockSz, blockSz, blockSz);
    ctx.strokeStyle = '#0369A1';
    ctx.lineWidth = 2;
    ctx.strokeRect(-blockSz / 2, -blockSz, blockSz, blockSz);

    ctx.fillStyle = '#FFFFFF';
    ctx.font = 'bold 11px -apple-system, sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText(mass.toFixed(1) + ' kg', 0, -blockSz / 2 + 4);
    ctx.restore();

    // Vector FBD
    const fScale = 1.4;
    const fbdX = blockX - (blockSz / 2) * Math.sin(rad);
    const fbdY = blockY - (blockSz / 2) * Math.cos(rad);

    // 1. Gravity W (straight down in red)
    drawArrow(fbdX, fbdY, fbdX, fbdY + W * fScale, '#DC2626', 'W (Berat)');
    // 2. Normal Force N (perpendicular upward in green)
    const nMag = Wperp * fScale;
    const nx = fbdX - Math.sin(rad) * nMag;
    const ny = fbdY - Math.cos(rad) * nMag;
    drawArrow(fbdX, fbdY, nx, ny, '#059669', 'N (Normal)');
    // 3. Friction f (up the ramp in amber)
    const fMag = friction * fScale;
    const fx = fbdX + Math.cos(rad) * fMag;
    const fy = fbdY - Math.sin(rad) * fMag;
    if (fMag > 1) {
      drawArrow(fbdX, fbdY, fx, fy, '#D97706', 'f (Gesek)');
    }

    requestAnimationFrame(step);
  }
  requestAnimationFrame(step);
})();
</script>
</body>
</html>
"""

    fun getHookeSpringHtml(): String = """
<!DOCTYPE html>
<html lang="id">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; -webkit-tap-highlight-color: transparent; }
  html, body { position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; margin: 0; padding: 0; overflow: hidden; display: flex; flex-direction: column; background: #FFFFFF; color: #0F172A; user-select: none; -webkit-user-select: none; }
  #canvas-container { flex: 1 1 0%; min-height: 0; position: relative; width: 100%; background: #FFFFFF; overflow: hidden; }
  canvas { position: absolute; top: 0; left: 0; width: 100%; height: 100%; display: block; touch-action: none; }
  #glass-hud { position: absolute; top: 14px; left: 14px; background: rgba(255, 255, 255, 0.94); backdrop-filter: blur(8px); border: 1px solid #E2E8F0; border-radius: 12px; padding: 10px 14px; box-shadow: 0 4px 14px rgba(0,0,0,0.06); font-size: 12px; line-height: 1.6; max-width: 320px; pointer-events: none; z-index: 5; }
  .hud-row { display: flex; justify-content: space-between; gap: 12px; }
  .hud-val { font-weight: 700; color: #0284C7; font-family: ui-monospace, Menlo, Consolas, monospace; }
  #control-dock { flex-shrink: 0; background: #F8FAFC; border-top: 1px solid #E2E8F0; padding: 12px 18px; display: flex; flex-direction: column; gap: 10px; z-index: 10; }
  .sliders-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 12px; }
  .control-group { display: flex; flex-direction: column; gap: 4px; }
  .control-label { display: flex; justify-content: space-between; font-size: 11px; font-weight: 600; color: #64748B; }
  .val-badge { font-weight: 700; color: #0284C7; font-family: ui-monospace, Menlo, monospace; }
  input[type="range"] { width: 100%; height: 26px; accent-color: #0284C7; cursor: pointer; }
  .actions-row { display: flex; align-items: center; justify-content: space-between; gap: 10px; border-top: 1px solid #E2E8F0; padding-top: 8px; }
  .btn-group { display: flex; gap: 8px; }
  button { padding: 8px 16px; font-size: 12px; font-weight: 600; border-radius: 8px; cursor: pointer; border: 1px solid #CBD5E1; background: #FFFFFF; color: #0F172A; }
  button.active { background: #0F172A; color: #FFFFFF; border-color: #0F172A; }
  .formula-box { font-family: ui-monospace, Menlo, Consolas, monospace; font-size: 11px; color: #334155; background: #FFFFFF; border: 1px solid #E2E8F0; border-radius: 6px; padding: 6px 12px; }

  /* ── Studio Workbench Layout Upgrade ── */
  html, body {
    position: fixed !important;
    inset: 0 !important;
    width: 100vw !important;
    height: 100vh !important;
    margin: 0 !important;
    padding: 0 !important;
    overflow: hidden !important;
    display: flex !important;
    flex-direction: row !important;
    background: #FFFFFF !important;
    color: #0F172A !important;
    user-select: none !important;
    -webkit-user-select: none !important;
  }
  #canvas-container, #main-container {
    flex: 1 1 0% !important;
    min-width: 0 !important;
    height: 100% !important;
    position: relative !important;
    background: #FFFFFF !important;
    overflow: hidden !important;
  }
  #control-dock, #inspector-sidebar {
    width: 340px !important;
    max-width: 35vw !important;
    flex-shrink: 0 !important;
    height: 100% !important;
    background: #F8FAFC !important;
    border-left: 1px solid #E2E8F0 !important;
    border-top: none !important;
    padding: 20px 18px !important;
    display: flex !important;
    flex-direction: column !important;
    gap: 14px !important;
    overflow-y: auto !important;
    z-index: 10 !important;
    box-shadow: -4px 0 16px rgba(0,0,0,0.02) !important;
  }
  .sliders-grid {
    display: flex !important;
    flex-direction: column !important;
    gap: 12px !important;
  }
  .control-group {
    display: flex !important;
    flex-direction: column !important;
    gap: 6px !important;
    background: #FFFFFF !important;
    border: 1px solid #E2E8F0 !important;
    border-radius: 10px !important;
    padding: 10px 12px !important;
    box-shadow: 0 1px 3px rgba(0,0,0,0.02) !important;
  }
  .actions-row {
    display: flex !important;
    flex-direction: column !important;
    gap: 10px !important;
    border-top: 1px solid #E2E8F0 !important;
    padding-top: 14px !important;
    margin-top: auto !important;
  }
  .btn-group {
    display: flex !important;
    gap: 8px !important;
    width: 100% !important;
  }
  .btn-group button {
    flex: 1 !important;
  }
  .formula-box, .theory-card {
    font-family: ui-monospace, Menlo, Consolas, monospace !important;
    font-size: 11px !important;
    color: #0369A1 !important;
    background: #EFF6FF !important;
    border: 1px solid #BFDBFE !important;
    border-radius: 8px !important;
    padding: 10px 12px !important;
    line-height: 1.5 !important;
    word-break: break-word !important;
  }

</style>
</head>
<body>
<div id="canvas-container">
  <canvas id="simCanvas"></canvas>
  <div id="glass-hud">
    <div class="hud-row"><span>Status:</span><span id="hudStatus" class="hud-val">Setimbang</span></div>
    <div class="hud-row"><span>Gaya Tarik (F = m·g):</span><span id="hudForce" class="hud-val">1.96 N</span></div>
    <div class="hud-row"><span>Pertambahan Panjang (Δx):</span><span id="hudDeltaX" class="hud-val">0.039 m (3.9 cm)</span></div>
    <div class="hud-row"><span>Energi Potensial Pegas (Ep):</span><span id="hudEnergy" class="hud-val">0.038 J</span></div>
    <div class="hud-row"><span>Periode Osilasi (T):</span><span id="hudPeriod" class="hud-val">0.40 s</span></div>
  </div>
</div>
<div id="control-dock">
  <div class="sliders-grid">
    <div class="control-group">
      <div class="control-label"><span>Konstanta Pegas (k)</span><span class="val-badge" id="valK">50 N/m</span></div>
      <input type="range" id="rangeK" min="20" max="200" step="5" value="50">
    </div>
    <div class="control-group">
      <div class="control-label"><span>Massa Beban (m)</span><span class="val-badge" id="valMass">200 g (0.20 kg)</span></div>
      <input type="range" id="rangeMass" min="50" max="500" step="10" value="200">
    </div>
    <div class="control-group">
      <div class="control-label"><span>Peredam Getaran</span><span class="val-badge" id="valDamp">Sedang (0.04)</span></div>
      <input type="range" id="rangeDamp" min="0" max="0.15" step="0.01" value="0.04">
    </div>
    <div class="control-group">
      <div class="control-label"><span>Osilasi Harmonik</span><span class="val-badge" id="valMode">Tarik Beban</span></div>
      <div class="btn-group">
        <button id="btnOscillate" style="flex:1;">Tarik & Lepas Beban</button>
      </div>
    </div>
  </div>
  <div class="actions-row">
    <div class="btn-group">
      <button id="btnDampen">Hentikan Gerak</button>
      <button id="btnReset">Reset Default</button>
    </div>
    <div class="formula-box" id="formulaPill">
      Hukum Hooke: F = k · Δx | Ep = ½ · k · (Δx)² | T = 2π√(m/k)
    </div>
  </div>
</div>

<script>
(function() {
  const container = document.getElementById('canvas-container');
  const canvas = document.getElementById('simCanvas');
  const ctx = canvas.getContext('2d');

  let width = 600, height = 400;
  function resize() {
    const w = container.clientWidth || 600;
    const h = container.clientHeight || 400;
    const dpr = window.devicePixelRatio || 1;
    canvas.width = Math.round(w * dpr);
    canvas.height = Math.round(h * dpr);
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    width = w;
    height = h;
  }
  window.addEventListener('resize', resize);
  if (window.ResizeObserver) {
    new ResizeObserver(resize).observe(container);
  }
  resize();

  // Physics constants
  const g = 9.80; // m/s^2

  // Parameters
  let k = 50.0; // N/m
  let massGrams = 200.0; // g
  let damping = 0.04;

  // Dynamic simulation state
  // Equilibrium stretch: deltaX_eq = (m * g) / k
  // Displacement y (in meters) relative to equilibrium
  let yDisplacement = 0; // m (relative to eq)
  let yVelocity = 0; // m/s
  let isDraggingMass = false;

  // DOM
  const rangeK = document.getElementById('rangeK');
  const rangeMass = document.getElementById('rangeMass');
  const rangeDamp = document.getElementById('rangeDamp');
  const valK = document.getElementById('valK');
  const valMass = document.getElementById('valMass');
  const valDamp = document.getElementById('valDamp');
  const valMode = document.getElementById('valMode');
  const hudStatus = document.getElementById('hudStatus');
  const hudForce = document.getElementById('hudForce');
  const hudDeltaX = document.getElementById('hudDeltaX');
  const hudEnergy = document.getElementById('hudEnergy');
  const hudPeriod = document.getElementById('hudPeriod');
  const formulaPill = document.getElementById('formulaPill');
  const btnOscillate = document.getElementById('btnOscillate');
  const btnDampen = document.getElementById('btnDampen');
  const btnReset = document.getElementById('btnReset');

  function updateHUD() {
    const massKg = massGrams / 1000.0;
    valK.textContent = k.toFixed(0) + ' N/m';
    valMass.textContent = massGrams.toFixed(0) + ' g (' + massKg.toFixed(2) + ' kg)';
    valDamp.textContent = damping === 0 ? 'Tanpa Gesekan' : (damping < 0.06 ? 'Sedang' : 'Tinggi');

    const eqDeltaX = (massKg * g) / k;
    const currentDeltaX = eqDeltaX + yDisplacement;
    const force = k * currentDeltaX;
    const ep = 0.5 * k * currentDeltaX * currentDeltaX;
    const period = 2 * Math.PI * Math.sqrt(massKg / k);

    hudStatus.textContent = Math.abs(yVelocity) > 0.02 ? 'Bergetar (Osilasi)' : 'Setimbang';
    hudStatus.style.color = Math.abs(yVelocity) > 0.02 ? '#0284C7' : '#059669';
    hudForce.textContent = force.toFixed(2) + ' N';
    hudDeltaX.textContent = currentDeltaX.toFixed(3) + ' m (' + (currentDeltaX * 100).toFixed(1) + ' cm)';
    hudEnergy.textContent = ep.toFixed(3) + ' J';
    hudPeriod.textContent = period.toFixed(2) + ' s (f = ' + (1 / period).toFixed(2) + ' Hz)';

    formulaPill.textContent = 'F = ' + k.toFixed(0) + ' · ' + currentDeltaX.toFixed(3) + 'm = ' + force.toFixed(2) + ' N | Ep = ' + ep.toFixed(3) + ' J | T = ' + period.toFixed(2) + ' s';
  }
  updateHUD();

  rangeK.oninput = (e) => { k = parseFloat(e.target.value); updateHUD(); };
  rangeMass.oninput = (e) => { massGrams = parseFloat(e.target.value); updateHUD(); };
  rangeDamp.oninput = (e) => { damping = parseFloat(e.target.value); updateHUD(); };

  btnOscillate.onclick = () => {
    yDisplacement = 0.08; // pull down 8 cm
    yVelocity = 0;
    updateHUD();
  };

  btnDampen.onclick = () => {
    yDisplacement = 0;
    yVelocity = 0;
    updateHUD();
  };

  btnReset.onclick = () => {
    k = 50.0;
    massGrams = 200.0;
    damping = 0.04;
    yDisplacement = 0;
    yVelocity = 0;
    rangeK.value = 50;
    rangeMass.value = 200;
    rangeDamp.value = 0.04;
    updateHUD();
  };

  // Pointer drag for mass block
  let dragStartY = 0;
  let dragStartDisp = 0;

  canvas.addEventListener('pointerdown', (e) => {
    const rect = canvas.getBoundingClientRect();
    const mx = e.clientX - rect.left;
    const my = e.clientY - rect.top;

    // Check hit near mass block
    const massKg = massGrams / 1000.0;
    const eqDeltaX = (massKg * g) / k;
    const currentDeltaX = eqDeltaX + yDisplacement;
    const pixelsPerMeter = 600;
    const springAnchorY = 60;
    const naturalLenPx = 110;
    const massCenterY = springAnchorY + naturalLenPx + (currentDeltaX * pixelsPerMeter);
    const springAnchorX = Math.min(220, width * 0.3);

    if (Math.abs(mx - springAnchorX) < 60 && Math.abs(my - massCenterY) < 50) {
      isDraggingMass = true;
      dragStartY = my;
      dragStartDisp = yDisplacement;
      yVelocity = 0;
    }
  });

  window.addEventListener('pointermove', (e) => {
    if (!isDraggingMass) return;
    const rect = canvas.getBoundingClientRect();
    const my = e.clientY - rect.top;
    const dyPx = my - dragStartY;
    const pixelsPerMeter = 600;
    yDisplacement = dragStartDisp + (dyPx / pixelsPerMeter);
    yDisplacement = Math.max(-0.12, Math.min(0.25, yDisplacement));
    yVelocity = 0;
    updateHUD();
  });

  window.addEventListener('pointerup', () => {
    if (isDraggingMass) {
      isDraggingMass = false;
    }
  });

  // Animation physics loop
  let lastTime = performance.now();
  function draw(ts) {
    const dt = Math.min(0.04, (ts - lastTime) / 1000);
    lastTime = ts;

    const massKg = massGrams / 1000.0;
    const eqDeltaX = (massKg * g) / k;

    if (!isDraggingMass) {
      // Harmonic oscillator ODE: m * y'' = -k * y - b * y'
      const accel = (-k * yDisplacement - damping * 80 * yVelocity) / massKg;
      yVelocity += accel * dt;
      yDisplacement += yVelocity * dt;

      if (Math.abs(yDisplacement) < 0.0005 && Math.abs(yVelocity) < 0.001) {
        yDisplacement = 0;
        yVelocity = 0;
      }
    }

    ctx.clearRect(0, 0, width, height);

    // Spring rig layout
    const pixelsPerMeter = 650;
    const anchorX = Math.min(200, width * 0.32);
    const anchorY = 55;
    const naturalLenPx = 100;
    const currentDeltaX = eqDeltaX + yDisplacement;
    const currentSpringLenPx = Math.max(30, naturalLenPx + currentDeltaX * pixelsPerMeter);
    const massY = anchorY + currentSpringLenPx;

    // 1. Support Beam / Stand
    ctx.fillStyle = '#64748B';
    ctx.fillRect(anchorX - 70, anchorY - 14, 140, 14);
    ctx.fillStyle = '#475569';
    ctx.fillRect(anchorX - 10, anchorY - 14, 20, 14);
    ctx.fillStyle = '#94A3B8';
    ctx.beginPath();
    ctx.arc(anchorX, anchorY, 4, 0, Math.PI * 2);
    ctx.fill();

    // 2. Hanging Spring Coils
    const coils = 16;
    const coilRadius = 18;
    ctx.strokeStyle = '#0284C7';
    ctx.lineWidth = 3.5;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';
    ctx.beginPath();
    ctx.moveTo(anchorX, anchorY);

    for (let i = 0; i <= coils * 2; i++) {
      const u = i / (coils * 2);
      const cy = anchorY + u * currentSpringLenPx;
      const cx = anchorX + (i % 2 === 0 ? -coilRadius : coilRadius);
      ctx.lineTo(cx, cy);
    }
    ctx.lineTo(anchorX, massY);
    ctx.stroke();

    // 3. Mass Block
    const mw = 65;
    const mh = 48;
    ctx.fillStyle = '#D97706';
    ctx.fillRect(anchorX - mw / 2, massY, mw, mh);
    ctx.strokeStyle = '#B45309';
    ctx.lineWidth = 2;
    ctx.strokeRect(anchorX - mw / 2, massY, mw, mh);

    // Mass Hook
    ctx.strokeStyle = '#475569';
    ctx.lineWidth = 3;
    ctx.beginPath();
    ctx.arc(anchorX, massY - 6, 6, 0, Math.PI);
    ctx.stroke();

    // Mass Label
    ctx.fillStyle = '#FFFFFF';
    ctx.font = 'bold 12px ui-monospace, Menlo, monospace';
    ctx.textAlign = 'center';
    ctx.fillText(massGrams.toFixed(0) + ' g', anchorX, massY + 28);
    ctx.font = '9px -apple-system, sans-serif';
    ctx.fillText('Tarik Saya', anchorX, massY + 41);
    ctx.textAlign = 'left';

    // 4. Lab Ruler on left of spring
    const rulerX = anchorX - 85;
    const rulerTop = anchorY;
    const rulerHeight = Math.min(height - 70, 360);
    ctx.fillStyle = '#FEF3C7';
    ctx.strokeStyle = '#D97706';
    ctx.lineWidth = 1;
    ctx.fillRect(rulerX, rulerTop, 24, rulerHeight);
    ctx.strokeRect(rulerX, rulerTop, 24, rulerHeight);

    ctx.fillStyle = '#78350F';
    ctx.font = '8px ui-monospace, Menlo, monospace';
    for (let cm = 0; cm <= 40; cm += 2) {
      const py = rulerTop + (cm / 100) * pixelsPerMeter;
      if (py > rulerTop + rulerHeight) break;
      const isMajor = (cm % 10 === 0);
      const tickW = isMajor ? 12 : 6;
      ctx.fillRect(rulerX + 24 - tickW, py, tickW, 1);
      if (isMajor && cm > 0) {
        ctx.fillText(cm + '', rulerX + 3, py + 3);
      }
    }

    // 5. Reference Alignment Dashed Lines
    // Posisi awal pegas tanpa beban (x0)
    const y0 = anchorY + naturalLenPx;
    ctx.strokeStyle = 'rgba(100, 116, 139, 0.4)';
    ctx.setLineDash([4, 4]);
    ctx.beginPath();
    ctx.moveTo(rulerX + 24, y0);
    ctx.lineTo(anchorX + 80, y0);
    ctx.stroke();

    // Posisi Setimbang (x_eq)
    const yEq = anchorY + naturalLenPx + eqDeltaX * pixelsPerMeter;
    ctx.strokeStyle = 'rgba(2, 132, 199, 0.6)';
    ctx.beginPath();
    ctx.moveTo(rulerX + 24, yEq);
    ctx.lineTo(anchorX + 80, yEq);
    ctx.stroke();

    // Posisi Saat Ini (x)
    ctx.strokeStyle = 'rgba(217, 119, 6, 0.8)';
    ctx.beginPath();
    ctx.moveTo(rulerX + 24, massY);
    ctx.lineTo(anchorX + 80, massY);
    ctx.stroke();
    ctx.setLineDash([]);

    // Delta x dimension indicator
    ctx.strokeStyle = '#D97706';
    ctx.lineWidth = 1.5;
    const dimX = anchorX + 65;
    ctx.beginPath();
    ctx.moveTo(dimX, y0);
    ctx.lineTo(dimX, massY);
    ctx.stroke();
    // End arrows
    ctx.fillStyle = '#D97706';
    ctx.beginPath();
    ctx.arc(dimX, y0, 3, 0, Math.PI * 2);
    ctx.arc(dimX, massY, 3, 0, Math.PI * 2);
    ctx.fill();

    ctx.font = 'bold 11px ui-monospace, Menlo, monospace';
    ctx.fillText('Δx = ' + (currentDeltaX * 100).toFixed(1) + ' cm', dimX + 8, (y0 + massY) / 2 + 4);

    // 6. Right Side: Interactive Hooke's Law Chart (F vs deltaX)
    const chartX = Math.max(anchorX + 160, width * 0.52);
    const chartY = 40;
    const chartW = width - chartX - 25;
    const chartH = height - chartY - 30;

    if (chartW > 140) {
      // Chart Background
      ctx.fillStyle = '#F8FAFC';
      ctx.strokeStyle = '#E2E8F0';
      ctx.lineWidth = 1;
      ctx.fillRect(chartX, chartY, chartW, chartH);
      ctx.strokeRect(chartX, chartY, chartW, chartH);

      // Chart Title
      ctx.fillStyle = '#0F172A';
      ctx.font = 'bold 11px -apple-system, sans-serif';
      ctx.fillText('Grafik Gaya Pegas (Hukum Hooke: F = k·Δx)', chartX + 10, chartY + 18);

      const maxDxChart = 0.25; // 25 cm
      const maxFChart = k * maxDxChart * 1.1; // N
      const plotX = chartX + 35;
      const plotY = chartY + 30;
      const plotW = chartW - 45;
      const plotH = chartH - 55;

      // Axes
      ctx.strokeStyle = '#94A3B8';
      ctx.lineWidth = 1.5;
      ctx.beginPath();
      ctx.moveTo(plotX, plotY);
      ctx.lineTo(plotX, plotY + plotH);
      ctx.lineTo(plotX + plotW, plotY + plotH);
      ctx.stroke();

      // Axis labels
      ctx.fillStyle = '#64748B';
      ctx.font = '9px ui-monospace, Menlo, monospace';
      ctx.fillText('F (N)', plotX - 28, plotY + 10);
      ctx.fillText('Δx (cm)', plotX + plotW - 35, plotY + plotH + 16);

      // Linear Line: F = k * deltaX
      ctx.strokeStyle = '#0284C7';
      ctx.lineWidth = 2.5;
      ctx.beginPath();
      ctx.moveTo(plotX, plotY + plotH);
      const xEndPx = plotX + plotW;
      const yEndPx = plotY + plotH - (plotH * (k * maxDxChart) / maxFChart);
      ctx.lineTo(xEndPx, yEndPx);
      ctx.stroke();

      // Current Operating Point on Chart
      const curDxClamped = Math.max(0, Math.min(maxDxChart, currentDeltaX));
      const curPtX = plotX + (curDxClamped / maxDxChart) * plotW;
      const curPtY = plotY + plotH - ((k * curDxClamped) / maxFChart) * plotH;

      ctx.fillStyle = '#DC2626';
      ctx.beginPath();
      ctx.arc(curPtX, curPtY, 5, 0, Math.PI * 2);
      ctx.fill();
      ctx.strokeStyle = '#FFFFFF';
      ctx.lineWidth = 2;
      ctx.stroke();

      // Operating Point Label
      ctx.fillStyle = '#DC2626';
      ctx.font = 'bold 10px ui-monospace, Menlo, monospace';
      ctx.fillText('(' + (curDxClamped * 100).toFixed(1) + ' cm, ' + (k * curDxClamped).toFixed(2) + ' N)', curPtX - 30, curPtY - 10);

      // Energy Under Curve Area (shaded triangle)
      ctx.fillStyle = 'rgba(2, 132, 199, 0.12)';
      ctx.beginPath();
      ctx.moveTo(plotX, plotY + plotH);
      ctx.lineTo(curPtX, curPtY);
      ctx.lineTo(curPtX, plotY + plotH);
      ctx.closePath();
      ctx.fill();

      // Area label (Ep)
      if (curPtX > plotX + 40) {
        ctx.fillStyle = '#0369A1';
        ctx.font = '10px ui-monospace, Menlo, monospace';
        ctx.fillText('Ep = ' + (0.5 * k * curDxClamped * curDxClamped).toFixed(3) + ' J', plotX + (curPtX - plotX) * 0.35, plotY + plotH - 10);
      }
    }

    if (Math.abs(yVelocity) > 0.005) {
      updateHUD();
    }

    requestAnimationFrame(draw);
  }

  draw(performance.now());
})();
</script>
</body>
</html>
"""

    fun getProjectileSimulationHtml(): String = """
﻿<!DOCTYPE html>
<html lang="id">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; -webkit-tap-highlight-color: transparent; }
  html, body { width: 100%; height: 100%; overflow: hidden; display: flex; flex-direction: column; background: #FFFFFF; color: #0F172A; user-select: none; -webkit-user-select: none; }
  #canvas-container { flex: 1 1 0%; min-height: 0; position: relative; width: 100%; background: #FFFFFF; overflow: hidden; }
  canvas { position: absolute; top: 0; left: 0; width: 100%; height: 100%; display: block; touch-action: none; }
  #glass-hud { position: absolute; top: 14px; left: 14px; background: rgba(255, 255, 255, 0.94); backdrop-filter: blur(8px); border: 1px solid #E2E8F0; border-radius: 12px; padding: 10px 14px; box-shadow: 0 4px 14px rgba(0,0,0,0.06); font-size: 12px; line-height: 1.6; max-width: 320px; pointer-events: none; z-index: 5; }
  .hud-row { display: flex; justify-content: space-between; gap: 12px; }
  .hud-val { font-weight: 700; color: #0284C7; font-family: ui-monospace, Menlo, Consolas, monospace; }
  #control-dock { flex-shrink: 0; background: #F8FAFC; border-top: 1px solid #E2E8F0; padding: 12px 18px; display: flex; flex-direction: column; gap: 10px; z-index: 10; }
  .sliders-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 12px; }
  .control-group { display: flex; flex-direction: column; gap: 4px; }
  .control-label { display: flex; justify-content: space-between; font-size: 11px; font-weight: 600; color: #64748B; }
  .val-badge { font-weight: 700; color: #0284C7; font-family: ui-monospace, Menlo, monospace; }
  input[type="range"] { width: 100%; height: 26px; accent-color: #0284C7; cursor: pointer; }
  select { background: #FFFFFF; border: 1px solid #CBD5E1; border-radius: 6px; padding: 4px 8px; font-size: 12px; font-weight: 600; color: #0F172A; }
  .actions-row { display: flex; align-items: center; justify-content: space-between; gap: 10px; border-top: 1px solid #E2E8F0; padding-top: 8px; }
  .btn-group { display: flex; gap: 8px; }
  button { padding: 8px 16px; font-size: 12px; font-weight: 600; border-radius: 8px; cursor: pointer; border: 1px solid #CBD5E1; background: #FFFFFF; color: #0F172A; }
  button.active { background: #0F172A; color: #FFFFFF; border-color: #0F172A; }
  .formula-box { font-family: ui-monospace, Menlo, Consolas, monospace; font-size: 11px; color: #334155; background: #FFFFFF; border: 1px solid #E2E8F0; border-radius: 6px; padding: 6px 12px; }

  /* ── Studio Workbench Layout Upgrade ── */
  html, body {
    position: fixed !important;
    inset: 0 !important;
    width: 100vw !important;
    height: 100vh !important;
    margin: 0 !important;
    padding: 0 !important;
    overflow: hidden !important;
    display: flex !important;
    flex-direction: row !important;
    background: #FFFFFF !important;
    color: #0F172A !important;
    user-select: none !important;
    -webkit-user-select: none !important;
  }
  #canvas-container, #main-container {
    flex: 1 1 0% !important;
    min-width: 0 !important;
    height: 100% !important;
    position: relative !important;
    background: #FFFFFF !important;
    overflow: hidden !important;
  }
  #control-dock, #inspector-sidebar {
    width: 340px !important;
    max-width: 35vw !important;
    flex-shrink: 0 !important;
    height: 100% !important;
    background: #F8FAFC !important;
    border-left: 1px solid #E2E8F0 !important;
    border-top: none !important;
    padding: 20px 18px !important;
    display: flex !important;
    flex-direction: column !important;
    gap: 14px !important;
    overflow-y: auto !important;
    z-index: 10 !important;
    box-shadow: -4px 0 16px rgba(0,0,0,0.02) !important;
  }
  .sliders-grid {
    display: flex !important;
    flex-direction: column !important;
    gap: 12px !important;
  }
  .control-group {
    display: flex !important;
    flex-direction: column !important;
    gap: 6px !important;
    background: #FFFFFF !important;
    border: 1px solid #E2E8F0 !important;
    border-radius: 10px !important;
    padding: 10px 12px !important;
    box-shadow: 0 1px 3px rgba(0,0,0,0.02) !important;
  }
  .actions-row {
    display: flex !important;
    flex-direction: column !important;
    gap: 10px !important;
    border-top: 1px solid #E2E8F0 !important;
    padding-top: 14px !important;
    margin-top: auto !important;
  }
  .btn-group {
    display: flex !important;
    gap: 8px !important;
    width: 100% !important;
  }
  .btn-group button {
    flex: 1 !important;
  }
  .formula-box, .theory-card {
    font-family: ui-monospace, Menlo, Consolas, monospace !important;
    font-size: 11px !important;
    color: #0369A1 !important;
    background: #EFF6FF !important;
    border: 1px solid #BFDBFE !important;
    border-radius: 8px !important;
    padding: 10px 12px !important;
    line-height: 1.5 !important;
    word-break: break-word !important;
  }

</style>
</head>
<body>
<div id="canvas-container">
  <canvas id="simCanvas"></canvas>
  <div id="glass-hud">
    <div class="hud-row"><span>Status:</span><span id="hudStatus" class="hud-val">Siap Diluncurkan</span></div>
    <div class="hud-row"><span>Waktu Terbang (t):</span><span id="hudTime" class="hud-val">0.00 s</span></div>
    <div class="hud-row"><span>Tinggi Puncak (H):</span><span id="hudPeak" class="hud-val">0.00 m</span></div>
    <div class="hud-row"><span>Jangkauan Terjauh (R):</span><span id="hudRange" class="hud-val">0.00 m</span></div>
  </div>
</div>
<div id="control-dock">
  <div class="sliders-grid">
    <div class="control-group">
      <div class="control-label"><span>Sudut Elevasi (θ)</span><span class="val-badge" id="valAngle">45.0°</span></div>
      <input type="range" id="rangeAngle" min="10" max="85" step="1" value="45">
    </div>
    <div class="control-group">
      <div class="control-label"><span>Kecepatan Awal (v₀)</span><span class="val-badge" id="valSpeed">35.0 m/s</span></div>
      <input type="range" id="rangeSpeed" min="10" max="60" step="1" value="35">
    </div>
    <div class="control-group">
      <div class="control-label"><span>Gravitasi (g)</span><span class="val-badge" id="valGrav">9.8 m/s²</span></div>
      <select id="selectGrav">
        <option value="9.8" selected>Bumi (9.8 m/s²)</option>
        <option value="1.6">Bulan (1.6 m/s²)</option>
        <option value="3.7">Mars (3.7 m/s²)</option>
        <option value="24.8">Jupiter (24.8 m/s²)</option>
      </select>
    </div>
    <div class="control-group">
      <div class="control-label"><span>Hambatan Udara (k)</span><span class="val-badge" id="valDrag">0.00 1/s</span></div>
      <input type="range" id="rangeDrag" min="0" max="0.10" step="0.01" value="0">
    </div>
  </div>
  <div class="actions-row">
    <div class="btn-group">
      <button id="btnLaunch" class="active">Luncurkan</button>
      <button id="btnReset">Reset Jejak</button>
    </div>
    <div class="formula-box" id="formulaPill">
      R = v₀² · sin(2θ) / g = 125.0 m | H_max = 31.25 m
    </div>
  </div>
</div>

<script>
(function() {
  const container = document.getElementById('canvas-container');
  const canvas = document.getElementById('simCanvas');
  const ctx = canvas.getContext('2d');

  let width = 600, height = 400;
  function resize() {
    const w = container.clientWidth || 600;
    const h = container.clientHeight || 400;
    const dpr = window.devicePixelRatio || 1;
    canvas.width = Math.round(w * dpr);
    canvas.height = Math.round(h * dpr);
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    width = w;
    height = h;
  }
  window.addEventListener('resize', resize);
  if (window.ResizeObserver) {
    new ResizeObserver(resize).observe(container);
  }
  resize();

  // Params
  let angleDeg = 45;
  let speed = 35;
  let gravity = 9.8;
  let drag = 0;

  // State
  let proj = null;
  let ghosts = [];

  const WORLD_W = 160; // meters horizontal view
  let scale = 1;
  let originX = 50;
  let originY = 350;

  function recomputeScale() {
    scale = (width - 80) / WORLD_W;
    originX = 50;
    originY = height - 40;
  }

  function w2sX(x) { return originX + x * scale; }
  function w2sY(y) { return originY - y * scale; }

  // Elements
  const rangeAngle = document.getElementById('rangeAngle');
  const rangeSpeed = document.getElementById('rangeSpeed');
  const selectGrav = document.getElementById('selectGrav');
  const rangeDrag = document.getElementById('rangeDrag');
  const valAngle = document.getElementById('valAngle');
  const valSpeed = document.getElementById('valSpeed');
  const valGrav = document.getElementById('valGrav');
  const valDrag = document.getElementById('valDrag');
  const hudStatus = document.getElementById('hudStatus');
  const hudTime = document.getElementById('hudTime');
  const hudPeak = document.getElementById('hudPeak');
  const hudRange = document.getElementById('hudRange');
  const formulaPill = document.getElementById('formulaPill');
  const btnLaunch = document.getElementById('btnLaunch');
  const btnReset = document.getElementById('btnReset');

  function updateFormula() {
    const aRad = (angleDeg * Math.PI) / 180;
    const v0 = speed;
    const g = gravity;
    const theoR = (v0 * v0 * Math.sin(2 * aRad)) / g;
    const theoH = (v0 * v0 * Math.sin(aRad) * Math.sin(aRad)) / (2 * g);
    formulaPill.textContent = 'R_teori = ' + theoR.toFixed(1) + ' m | H_teori = ' + theoH.toFixed(1) + ' m (k = ' + drag.toFixed(2) + ')';
  }
  updateFormula();

  function launch() {
    if (proj && proj.trail.length > 0) {
      ghosts.push(proj.trail);
      if (ghosts.length > 5) ghosts.shift();
    }
    const aRad = (angleDeg * Math.PI) / 180;
    proj = {
      x: 0,
      y: 0,
      vx: speed * Math.cos(aRad),
      vy: speed * Math.sin(aRad),
      trail: [{ x: 0, y: 0 }],
      flying: true,
      time: 0,
      peak: 0,
      range: 0
    };
    hudStatus.textContent = 'Mengudara';
    hudStatus.style.color = '#0284C7';
  }

  function reset() {
    proj = null;
    ghosts = [];
    hudStatus.textContent = 'Siap Diluncurkan';
    hudStatus.style.color = '#64748B';
    hudTime.textContent = '0.00 s';
    hudPeak.textContent = '0.00 m';
    hudRange.textContent = '0.00 m';
  }

  rangeAngle.oninput = (e) => {
    angleDeg = parseFloat(e.target.value);
    valAngle.textContent = angleDeg.toFixed(1) + '°';
    updateFormula();
  };
  rangeSpeed.oninput = (e) => {
    speed = parseFloat(e.target.value);
    valSpeed.textContent = speed.toFixed(1) + ' m/s';
    updateFormula();
  };
  selectGrav.onchange = (e) => {
    gravity = parseFloat(e.target.value);
    valGrav.textContent = gravity.toFixed(1) + ' m/s²';
    updateFormula();
  };
  rangeDrag.oninput = (e) => {
    drag = parseFloat(e.target.value);
    valDrag.textContent = drag.toFixed(2) + ' 1/s';
    updateFormula();
  };

  btnLaunch.onclick = launch;
  btnReset.onclick = reset;

  // Direct Drag Cannon Barrel to set Angle
  let isDraggingCannon = false;
  function getPos(e) {
    const rect = canvas.getBoundingClientRect();
    const cx = e.touches ? e.touches[0].clientX : e.clientX;
    const cy = e.touches ? e.touches[0].clientY : e.clientY;
    return {
      x: (cx - rect.left) * (canvas.width / rect.width) / (window.devicePixelRatio || 1),
      y: (cy - rect.top) * (canvas.height / rect.height) / (window.devicePixelRatio || 1)
    };
  }

  canvas.addEventListener('pointerdown', (e) => {
    const p = getPos(e);
    recomputeScale();
    const cannonLen = 50;
    const aRad = (angleDeg * Math.PI) / 180;
    const tipX = originX + cannonLen * Math.cos(aRad);
    const tipY = originY - cannonLen * Math.sin(aRad);
    if (Math.hypot(p.x - tipX, p.y - tipY) < 40) {
      isDraggingCannon = true;
      canvas.setPointerCapture(e.pointerId);
    }
  });

  canvas.addEventListener('pointermove', (e) => {
    if (!isDraggingCannon) return;
    const p = getPos(e);
    const dx = p.x - originX;
    const dy = originY - p.y;
    if (dx > 5 && dy > 5) {
      const angle = Math.atan2(dy, dx) * 180 / Math.PI;
      angleDeg = Math.round(Math.max(10, Math.min(85, angle)));
      rangeAngle.value = angleDeg;
      valAngle.textContent = angleDeg.toFixed(1) + '°';
      updateFormula();
    }
  });

  function endDrag() { isDraggingCannon = false; }
  canvas.addEventListener('pointerup', endDrag);
  canvas.addEventListener('pointercancel', endDrag);

  // Physics Step
  function step(dt) {
    if (!proj || !proj.flying) return;
    // Semi-implicit Euler with linear air drag
    proj.vx += -drag * proj.vx * dt;
    proj.vy += (-gravity - drag * proj.vy) * dt;
    proj.x += proj.vx * dt;
    proj.y += proj.vy * dt;
    proj.time += dt;

    if (proj.y > proj.peak) proj.peak = proj.y;

    if (proj.y <= 0 && proj.time > 0.05) {
      proj.y = 0;
      proj.flying = false;
      proj.range = proj.x;
      hudStatus.textContent = 'Mendarat';
      hudStatus.style.color = '#059669';
    }

    proj.trail.push({ x: proj.x, y: proj.y });
    if (proj.trail.length > 1500) proj.trail.shift();

    hudTime.textContent = proj.time.toFixed(2) + ' s';
    hudPeak.textContent = proj.peak.toFixed(2) + ' m';
    hudRange.textContent = (proj.flying ? proj.x : proj.range).toFixed(2) + ' m';
  }

  // Draw loop
  let lastTime = performance.now();
  function draw(ts) {
    const dt = Math.min(0.04, (ts - lastTime) / 1000);
    lastTime = ts;
    step(dt);
    recomputeScale();

    ctx.clearRect(0, 0, width, height);

    // Subtle background grid dots
    ctx.fillStyle = 'rgba(0, 0, 0, 0.04)';
    for (let x = 20; x < width; x += 24) {
      for (let y = 20; y < height; y += 24) {
        ctx.fillRect(x, y, 2, 2);
      }
    }

    // Distance tick markers on ground
    ctx.strokeStyle = '#CBD5E1';
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.moveTo(originX - 20, originY);
    ctx.lineTo(width - 20, originY);
    ctx.stroke();

    ctx.fillStyle = '#94A3B8';
    ctx.font = '10px ui-monospace, Menlo, monospace';
    ctx.textAlign = 'center';
    for (let m = 0; m <= WORLD_W; m += 20) {
      const sx = w2sX(m);
      if (sx > width - 30) break;
      ctx.beginPath();
      ctx.moveTo(sx, originY);
      ctx.lineTo(sx, originY + 6);
      ctx.stroke();
      ctx.fillText(m + 'm', sx, originY + 18);
    }

    // Draw previous ghost trails
    ctx.lineWidth = 1.5;
    ctx.setLineDash([3, 3]);
    ghosts.forEach((trail, idx) => {
      if (trail.length < 2) return;
      const alpha = 0.15 + (idx / ghosts.length) * 0.25;
      ctx.strokeStyle = 'rgba(100, 116, 139, ' + alpha + ')';
      ctx.beginPath();
      ctx.moveTo(w2sX(trail[0].x), w2sY(trail[0].y));
      for (let i = 1; i < trail.length; i++) {
        ctx.lineTo(w2sX(trail[i].x), w2sY(trail[i].y));
      }
      ctx.stroke();
    });
    ctx.setLineDash([]);

    // Draw active trajectory
    if (proj && proj.trail.length > 1) {
      ctx.strokeStyle = '#0284C7';
      ctx.lineWidth = 2.5;
      ctx.beginPath();
      ctx.moveTo(w2sX(proj.trail[0].x), w2sY(proj.trail[0].y));
      for (let i = 1; i < proj.trail.length; i++) {
        ctx.lineTo(w2sX(proj.trail[i].x), w2sY(proj.trail[i].y));
      }
      ctx.stroke();

      // Peak marker
      const peakX = proj.peak > 0 ? proj.trail.reduce((p, c) => c.y > p.y ? c : p, proj.trail[0]).x : 0;
      const spx = w2sX(peakX);
      const spy = w2sY(proj.peak);
      ctx.fillStyle = '#D97706';
      ctx.beginPath();
      ctx.arc(spx, spy, 4, 0, Math.PI * 2);
      ctx.fill();
      ctx.font = '10px ui-monospace, monospace';
      ctx.fillText('H_maks: ' + proj.peak.toFixed(1) + 'm', spx, spy - 8);
    }

    // Draw Cannon
    const cannonLen = 44;
    const aRad = (angleDeg * Math.PI) / 180;
    const tipX = originX + cannonLen * Math.cos(aRad);
    const tipY = originY - cannonLen * Math.sin(aRad);

    // Barrel
    ctx.strokeStyle = '#0F172A';
    ctx.lineWidth = 10;
    ctx.lineCap = 'round';
    ctx.beginPath();
    ctx.moveTo(originX, originY);
    ctx.lineTo(tipX, tipY);
    ctx.stroke();

    // Pivot mount
    ctx.fillStyle = '#475569';
    ctx.beginPath();
    ctx.arc(originX, originY, 14, Math.PI, 0, false);
    ctx.fill();

    // Interactive Drag Handle indicator at cannon tip
    ctx.fillStyle = 'rgba(2, 132, 199, 0.2)';
    ctx.beginPath();
    ctx.arc(tipX, tipY, 16, 0, Math.PI * 2);
    ctx.fill();
    ctx.strokeStyle = '#0284C7';
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.arc(tipX, tipY, 7, 0, Math.PI * 2);
    ctx.stroke();

    // Draw Projectile & Velocity Vectors
    if (proj && proj.flying) {
      const px = w2sX(proj.x);
      const py = w2sY(proj.y);

      // Ball
      ctx.fillStyle = '#0284C7';
      ctx.beginPath();
      ctx.arc(px, py, 6, 0, Math.PI * 2);
      ctx.fill();
      ctx.strokeStyle = '#FFFFFF';
      ctx.lineWidth = 1.5;
      ctx.stroke();

      // Velocity vector arrow
      const vScale = 0.8;
      const vxEnd = px + proj.vx * vScale;
      const vyEnd = py - proj.vy * vScale;

      ctx.strokeStyle = '#DC2626';
      ctx.lineWidth = 2;
      ctx.beginPath();
      ctx.moveTo(px, py);
      ctx.lineTo(vxEnd, vyEnd);
      ctx.stroke();

      // Arrowhead
      const headLen = 6;
      const vAng = Math.atan2(-proj.vy, proj.vx);
      ctx.fillStyle = '#DC2626';
      ctx.beginPath();
      ctx.moveTo(vxEnd, vyEnd);
      ctx.lineTo(vxEnd - headLen * Math.cos(vAng - Math.PI / 6), vyEnd - headLen * Math.sin(vAng - Math.PI / 6));
      ctx.lineTo(vxEnd - headLen * Math.cos(vAng + Math.PI / 6), vyEnd - headLen * Math.sin(vAng + Math.PI / 6));
      ctx.fill();
    }

    requestAnimationFrame(draw);
  }
  requestAnimationFrame(draw);
})();
</script>
</body>
</html>
"""

    fun getBohrAtomHtml(): String = """
<!DOCTYPE html>
<html lang="id">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; -webkit-tap-highlight-color: transparent; }
  html, body { position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; margin: 0; padding: 0; overflow: hidden; display: flex; flex-direction: column; background: #FFFFFF; color: #0F172A; user-select: none; -webkit-user-select: none; }
  #canvas-container { flex: 1 1 0%; min-height: 0; position: relative; width: 100%; background: #FFFFFF; overflow: hidden; }
  canvas { position: absolute; top: 0; left: 0; width: 100%; height: 100%; display: block; touch-action: none; }
  #glass-hud { position: absolute; top: 14px; left: 14px; background: rgba(255, 255, 255, 0.94); backdrop-filter: blur(8px); border: 1px solid #E2E8F0; border-radius: 12px; padding: 10px 14px; box-shadow: 0 4px 14px rgba(0,0,0,0.06); font-size: 12px; line-height: 1.6; max-width: 320px; pointer-events: none; z-index: 5; }
  .hud-row { display: flex; justify-content: space-between; gap: 12px; }
  .hud-val { font-weight: 700; color: #0284C7; font-family: ui-monospace, Menlo, Consolas, monospace; }
  #element-card { position: absolute; top: 14px; right: 14px; background: rgba(255, 255, 255, 0.94); backdrop-filter: blur(8px); border: 2px solid #0284C7; border-radius: 12px; padding: 12px 16px; box-shadow: 0 4px 14px rgba(0,0,0,0.06); text-align: center; pointer-events: none; z-index: 5; min-width: 110px; }
  .el-z { font-size: 11px; font-weight: 700; color: #64748B; font-family: ui-monospace, Menlo, monospace; }
  .el-symbol { font-size: 28px; font-weight: 800; color: #0284C7; line-height: 1.1; }
  .el-name { font-size: 11px; font-weight: 600; color: #0F172A; }
  .el-mass { font-size: 10px; color: #64748B; font-family: ui-monospace, Menlo, monospace; }
  #control-dock { flex-shrink: 0; background: #F8FAFC; border-top: 1px solid #E2E8F0; padding: 12px 18px; display: flex; flex-direction: column; gap: 10px; z-index: 10; }
  .sliders-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 12px; }
  .control-group { display: flex; flex-direction: column; gap: 4px; }
  .control-label { display: flex; justify-content: space-between; font-size: 11px; font-weight: 600; color: #64748B; }
  .val-badge { font-weight: 700; color: #0284C7; font-family: ui-monospace, Menlo, monospace; }
  input[type="range"] { width: 100%; height: 26px; accent-color: #0284C7; cursor: pointer; }
  select { width: 100%; padding: 6px 10px; font-size: 12px; font-weight: 600; border-radius: 8px; border: 1px solid #CBD5E1; background: #FFFFFF; color: #0F172A; }
  .actions-row { display: flex; align-items: center; justify-content: space-between; gap: 10px; border-top: 1px solid #E2E8F0; padding-top: 8px; }
  .btn-group { display: flex; gap: 8px; }
  button { padding: 8px 16px; font-size: 12px; font-weight: 600; border-radius: 8px; cursor: pointer; border: 1px solid #CBD5E1; background: #FFFFFF; color: #0F172A; }
  button.active { background: #0F172A; color: #FFFFFF; border-color: #0F172A; }
  .formula-box { font-family: ui-monospace, Menlo, Consolas, monospace; font-size: 11px; color: #334155; background: #FFFFFF; border: 1px solid #E2E8F0; border-radius: 6px; padding: 6px 12px; }

  /* ── Studio Workbench Layout Upgrade ── */
  html, body {
    position: fixed !important;
    inset: 0 !important;
    width: 100vw !important;
    height: 100vh !important;
    margin: 0 !important;
    padding: 0 !important;
    overflow: hidden !important;
    display: flex !important;
    flex-direction: row !important;
    background: #FFFFFF !important;
    color: #0F172A !important;
    user-select: none !important;
    -webkit-user-select: none !important;
  }
  #canvas-container, #main-container {
    flex: 1 1 0% !important;
    min-width: 0 !important;
    height: 100% !important;
    position: relative !important;
    background: #FFFFFF !important;
    overflow: hidden !important;
  }
  #control-dock, #inspector-sidebar {
    width: 340px !important;
    max-width: 35vw !important;
    flex-shrink: 0 !important;
    height: 100% !important;
    background: #F8FAFC !important;
    border-left: 1px solid #E2E8F0 !important;
    border-top: none !important;
    padding: 20px 18px !important;
    display: flex !important;
    flex-direction: column !important;
    gap: 14px !important;
    overflow-y: auto !important;
    z-index: 10 !important;
    box-shadow: -4px 0 16px rgba(0,0,0,0.02) !important;
  }
  .sliders-grid {
    display: flex !important;
    flex-direction: column !important;
    gap: 12px !important;
  }
  .control-group {
    display: flex !important;
    flex-direction: column !important;
    gap: 6px !important;
    background: #FFFFFF !important;
    border: 1px solid #E2E8F0 !important;
    border-radius: 10px !important;
    padding: 10px 12px !important;
    box-shadow: 0 1px 3px rgba(0,0,0,0.02) !important;
  }
  .actions-row {
    display: flex !important;
    flex-direction: column !important;
    gap: 10px !important;
    border-top: 1px solid #E2E8F0 !important;
    padding-top: 14px !important;
    margin-top: auto !important;
  }
  .btn-group {
    display: flex !important;
    gap: 8px !important;
    width: 100% !important;
  }
  .btn-group button {
    flex: 1 !important;
  }
  .formula-box, .theory-card {
    font-family: ui-monospace, Menlo, Consolas, monospace !important;
    font-size: 11px !important;
    color: #0369A1 !important;
    background: #EFF6FF !important;
    border: 1px solid #BFDBFE !important;
    border-radius: 8px !important;
    padding: 10px 12px !important;
    line-height: 1.5 !important;
    word-break: break-word !important;
  }

</style>
</head>
<body>
<div id="canvas-container">
  <canvas id="simCanvas"></canvas>
  <div id="glass-hud">
    <div class="hud-row"><span>Unsur:</span><span id="hudElem" class="hud-val">Natrium (Na)</span></div>
    <div class="hud-row"><span>Konfigurasi Kulit:</span><span id="hudShells" class="hud-val">2, 8, 1</span></div>
    <div class="hud-row"><span>Elektron Valensi:</span><span id="hudValence" class="hud-val" style="color: #D97706;">1 (Golongan IA)</span></div>
    <div class="hud-row"><span>Jumlah Kulit:</span><span id="hudNumShells" class="hud-val">3 (Periode 3)</span></div>
    <div class="hud-row"><span>Partikel Inti:</span><span id="hudNucleus" class="hud-val">11 p⁺, 12 n⁰</span></div>
  </div>
  <div id="element-card">
    <div class="el-z" id="cardZ">11</div>
    <div class="el-symbol" id="cardSymbol">Na</div>
    <div class="el-name" id="cardName">Natrium</div>
    <div class="el-mass" id="cardMass">22.99</div>
  </div>
</div>
<div id="control-dock">
  <div class="sliders-grid">
    <div class="control-group">
      <div class="control-label"><span>Pilih Unsur (Z = 1..36)</span><span class="val-badge" id="valZBadge">Z = 11</span></div>
      <select id="selectElement">
        <option value="1">1 - Hidrogen (H)</option>
        <option value="2">2 - Helium (He)</option>
        <option value="3">3 - Litium (Li)</option>
        <option value="6">6 - Karbon (C)</option>
        <option value="7">7 - Nitrogen (N)</option>
        <option value="8">8 - Oksigen (O)</option>
        <option value="9">9 - Fluor (F)</option>
        <option value="10">10 - Neon (Ne)</option>
        <option value="11" selected>11 - Natrium (Na)</option>
        <option value="12">12 - Magnesium (Mg)</option>
        <option value="13">13 - Aluminium (Al)</option>
        <option value="14">14 - Silikon (Si)</option>
        <option value="15">15 - Fosfor (P)</option>
        <option value="16">16 - Belerang (S)</option>
        <option value="17">17 - Klorin (Cl)</option>
        <option value="18">18 - Argon (Ar)</option>
        <option value="19">19 - Kalium (K)</option>
        <option value="20">20 - Kalsium (Ca)</option>
        <option value="26">26 - Besi (Fe)</option>
        <option value="29">29 - Tembaga (Cu)</option>
        <option value="35">35 - Bromin (Br)</option>
        <option value="36">36 - Kripton (Kr)</option>
      </select>
    </div>
    <div class="control-group">
      <div class="control-label"><span>Nomor Atom (Z)</span><span class="val-badge" id="valSliderZ">Z = 11</span></div>
      <input type="range" id="rangeZ" min="1" max="36" step="1" value="11">
    </div>
    <div class="control-group">
      <div class="control-label"><span>Kecepatan Orbit</span><span class="val-badge" id="valSpeed">1.0×</span></div>
      <input type="range" id="rangeSpeed" min="0.2" max="2.5" step="0.1" value="1.0">
    </div>
    <div class="control-group">
      <div class="control-label"><span>Animasi Gerak</span><span class="val-badge" id="valPlay">Berputar</span></div>
      <div class="btn-group">
        <button id="btnPlay" class="active" style="flex:1;">Jeda</button>
      </div>
    </div>
  </div>
  <div class="actions-row">
    <div class="btn-group">
      <button id="btnReset">Reset Default</button>
    </div>
    <div class="formula-box" id="formulaPill">
      Kapasitas Maksimal Kulit ke-n: 2n² (K=2, L=8, M=18, N=32)
    </div>
  </div>
</div>

<script>
(function() {
  const container = document.getElementById('canvas-container');
  const canvas = document.getElementById('simCanvas');
  const ctx = canvas.getContext('2d');

  let width = 600, height = 400;
  function resize() {
    const w = container.clientWidth || 600;
    const h = container.clientHeight || 400;
    const dpr = window.devicePixelRatio || 1;
    canvas.width = Math.round(w * dpr);
    canvas.height = Math.round(h * dpr);
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    width = w;
    height = h;
  }
  window.addEventListener('resize', resize);
  if (window.ResizeObserver) {
    new ResizeObserver(resize).observe(container);
  }
  resize();

  // Periodic Table Data (Z = 1..36)
  const ELEMENTS_DATA = [
    null,
    { z: 1, s: 'H', name: 'Hidrogen', m: 1.008, grp: 'IA', per: 1, n: 0 },
    { z: 2, s: 'He', name: 'Helium', m: 4.003, grp: 'VIIIA', per: 1, n: 2 },
    { z: 3, s: 'Li', name: 'Litium', m: 6.94, grp: 'IA', per: 2, n: 4 },
    { z: 4, s: 'Be', name: 'Berilium', m: 9.012, grp: 'IIA', per: 2, n: 5 },
    { z: 5, s: 'B', name: 'Boron', m: 10.81, grp: 'IIIA', per: 2, n: 6 },
    { z: 6, s: 'C', name: 'Karbon', m: 12.01, grp: 'IVA', per: 2, n: 6 },
    { z: 7, s: 'N', name: 'Nitrogen', m: 14.01, grp: 'VA', per: 2, n: 7 },
    { z: 8, s: 'O', name: 'Oksigen', m: 16.00, grp: 'VIA', per: 2, n: 8 },
    { z: 9, s: 'F', name: 'Fluorin', m: 19.00, grp: 'VIIA', per: 2, n: 10 },
    { z: 10, s: 'Ne', name: 'Neon', m: 20.18, grp: 'VIIIA', per: 2, n: 10 },
    { z: 11, s: 'Na', name: 'Natrium', m: 22.99, grp: 'IA', per: 3, n: 12 },
    { z: 12, s: 'Mg', name: 'Magnesium', m: 24.31, grp: 'IIA', per: 3, n: 12 },
    { z: 13, s: 'Al', name: 'Aluminium', m: 26.98, grp: 'IIIA', per: 3, n: 14 },
    { z: 14, s: 'Si', name: 'Silikon', m: 28.09, grp: 'IVA', per: 3, n: 14 },
    { z: 15, s: 'P', name: 'Fosfor', m: 30.97, grp: 'VA', per: 3, n: 16 },
    { z: 16, s: 'S', name: 'Belerang', m: 32.06, grp: 'VIA', per: 3, n: 16 },
    { z: 17, s: 'Cl', name: 'Klorin', m: 35.45, grp: 'VIIA', per: 3, n: 18 },
    { z: 18, s: 'Ar', name: 'Argon', m: 39.95, grp: 'VIIIA', per: 3, n: 22 },
    { z: 19, s: 'K', name: 'Kalium', m: 39.10, grp: 'IA', per: 4, n: 20 },
    { z: 20, s: 'Ca', name: 'Kalsium', m: 40.08, grp: 'IIA', per: 4, n: 20 },
    { z: 21, s: 'Sc', name: 'Skandium', m: 44.96, grp: 'IIIB', per: 4, n: 24 },
    { z: 22, s: 'Ti', name: 'Titanium', m: 47.87, grp: 'IVB', per: 4, n: 26 },
    { z: 23, s: 'V', name: 'Vanadium', m: 50.94, grp: 'VB', per: 4, n: 28 },
    { z: 24, s: 'Cr', name: 'Kromium', m: 52.00, grp: 'VIB', per: 4, n: 28 },
    { z: 25, s: 'Mn', name: 'Mangan', m: 54.94, grp: 'VIIB', per: 4, n: 30 },
    { z: 26, s: 'Fe', name: 'Besi', m: 55.85, grp: 'VIIIB', per: 4, n: 30 },
    { z: 27, s: 'Co', name: 'Kobalt', m: 58.93, grp: 'VIIIB', per: 4, n: 32 },
    { z: 28, s: 'Ni', name: 'Nikel', m: 58.69, grp: 'VIIIB', per: 4, n: 31 },
    { z: 29, s: 'Cu', name: 'Tembaga', m: 63.55, grp: 'IB', per: 4, n: 35 },
    { z: 30, s: 'Zn', name: 'Seng', m: 65.38, grp: 'IIB', per: 4, n: 35 },
    { z: 31, s: 'Ga', name: 'Galium', m: 69.72, grp: 'IIIA', per: 4, n: 39 },
    { z: 32, s: 'Ge', name: 'Germanium', m: 72.63, grp: 'IVA', per: 4, n: 41 },
    { z: 33, s: 'As', name: 'Arsen', m: 74.92, grp: 'VA', per: 4, n: 42 },
    { z: 34, s: 'Se', name: 'Selenium', m: 78.97, grp: 'VIA', per: 4, n: 45 },
    { z: 35, s: 'Br', name: 'Bromin', m: 79.90, grp: 'VIIA', per: 4, n: 45 },
    { z: 36, s: 'Kr', name: 'Kripton', m: 83.80, grp: 'VIIIA', per: 4, n: 48 }
  ];

  // Shell electron occupancy calculation
  const SUBSHELLS = [
    { n: 1, cap: 2 }, { n: 2, cap: 2 }, { n: 2, cap: 6 },
    { n: 3, cap: 2 }, { n: 3, cap: 6 }, { n: 4, cap: 2 },
    { n: 3, cap: 10 }, { n: 4, cap: 6 }
  ];
  const EXCEPTIONS = { 24: [4, 3], 29: [4, 3] }; // Cr, Cu Aufbau exceptions

  function getShells(z) {
    const perN = [];
    let rem = z;
    for (let i = 0; i < SUBSHELLS.length; i++) {
      if (rem <= 0) break;
      const sub = SUBSHELLS[i];
      const take = Math.min(rem, sub.cap);
      perN[sub.n - 1] = (perN[sub.n - 1] || 0) + take;
      rem -= take;
    }
    if (EXCEPTIONS[z]) {
      const ex = EXCEPTIONS[z];
      perN[ex[0] - 1] -= 1;
      perN[ex[1] - 1] += 1;
    }
    return perN.map((c) => c || 0);
  }

  // Params
  let currentZ = 11;
  let animSpeed = 1.0;
  let isPlaying = true;
  let simTime = 0;

  // DOM
  const selectElement = document.getElementById('selectElement');
  const rangeZ = document.getElementById('rangeZ');
  const rangeSpeed = document.getElementById('rangeSpeed');
  const valZBadge = document.getElementById('valZBadge');
  const valSliderZ = document.getElementById('valSliderZ');
  const valSpeed = document.getElementById('valSpeed');
  const valPlay = document.getElementById('valPlay');
  const hudElem = document.getElementById('hudElem');
  const hudShells = document.getElementById('hudShells');
  const hudValence = document.getElementById('hudValence');
  const hudNumShells = document.getElementById('hudNumShells');
  const hudNucleus = document.getElementById('hudNucleus');
  const cardZ = document.getElementById('cardZ');
  const cardSymbol = document.getElementById('cardSymbol');
  const cardName = document.getElementById('cardName');
  const cardMass = document.getElementById('cardMass');
  const formulaPill = document.getElementById('formulaPill');
  const btnPlay = document.getElementById('btnPlay');
  const btnReset = document.getElementById('btnReset');

  const shellNames = ['K', 'L', 'M', 'N'];

  function updateHUD() {
    const el = ELEMENTS_DATA[currentZ];
    const shells = getShells(currentZ);
    const valenceElectrons = shells[shells.length - 1];

    valZBadge.textContent = 'Z = ' + currentZ;
    valSliderZ.textContent = 'Z = ' + currentZ;
    valSpeed.textContent = animSpeed.toFixed(1) + '×';

    hudElem.textContent = el.name + ' (' + el.s + ')';
    hudShells.textContent = shells.join(', ') + ' (' + shells.map((n, i) => shellNames[i] + '=' + n).join(' ') + ')';
    hudValence.textContent = valenceElectrons + ' (Golongan ' + el.grp + ')';
    hudNumShells.textContent = shells.length + ' Kulit (Periode ' + el.per + ')';
    hudNucleus.textContent = el.z + ' p⁺, ' + el.n + ' n⁰ (Massa ≈ ' + Math.round(el.m) + ')';

    cardZ.textContent = el.z;
    cardSymbol.textContent = el.s;
    cardName.textContent = el.name;
    cardMass.textContent = el.m.toFixed(2);

    formulaPill.textContent = 'Konfigurasi Bohr ' + el.s + ' [Z=' + el.z + ']: ' + shells.join('-') + ' | e⁻ Valensi = ' + valenceElectrons;
  }
  updateHUD();

  function setZ(z) {
    currentZ = z;
    rangeZ.value = z;
    selectElement.value = z;
    updateHUD();
  }

  selectElement.onchange = (e) => { setZ(parseInt(e.target.value)); };
  rangeZ.oninput = (e) => { setZ(parseInt(e.target.value)); };
  rangeSpeed.oninput = (e) => {
    animSpeed = parseFloat(e.target.value);
    valSpeed.textContent = animSpeed.toFixed(1) + '×';
  };

  btnPlay.onclick = () => {
    isPlaying = !isPlaying;
    btnPlay.textContent = isPlaying ? 'Jeda' : 'Lanjut';
    btnPlay.classList.toggle('active', isPlaying);
    valPlay.textContent = isPlaying ? 'Berputar' : 'Dijeda';
  };

  btnReset.onclick = () => {
    setZ(11); // Natrium default
    animSpeed = 1.0;
    isPlaying = true;
    rangeSpeed.value = 1.0;
    btnPlay.textContent = 'Jeda';
    btnPlay.classList.add('active');
    updateHUD();
  };

  // Draw loop
  let lastTime = performance.now();
  function draw(ts) {
    const dt = Math.min(0.05, (ts - lastTime) / 1000);
    lastTime = ts;

    if (isPlaying) {
      simTime += dt * animSpeed;
    }

    ctx.clearRect(0, 0, width, height);

    const cx = width / 2;
    const cy = height / 2;
    const shells = getShells(currentZ);
    const numShells = shells.length;

    // Available radius
    const maxR = Math.min(width * 0.45, height * 0.44);
    const baseR = Math.max(38, maxR / (numShells + 1.2));
    const stepR = (maxR - baseR) / Math.max(1, numShells - 0.4);

    // 1. Orbital Ring Paths
    for (let s = 0; s < numShells; s++) {
      const r = baseR + s * stepR;
      ctx.strokeStyle = '#E2E8F0';
      ctx.lineWidth = 1.5;
      ctx.setLineDash([4, 4]);
      ctx.beginPath();
      ctx.arc(cx, cy, r, 0, Math.PI * 2);
      ctx.stroke();
      ctx.setLineDash([]);

      // Shell letter label (K, L, M, N)
      ctx.fillStyle = '#94A3B8';
      ctx.font = 'bold 10px ui-monospace, Menlo, monospace';
      ctx.fillText('Kulit ' + shellNames[s] + ' (n=' + (s + 1) + ')', cx + r + 6, cy - 4);
    }

    // 2. Central Nucleus (Protons + Neutrons)
    const el = ELEMENTS_DATA[currentZ];
    const nucRadius = Math.max(18, Math.min(26, 16 + Math.sqrt(currentZ)));

    // Outer glow
    const grad = ctx.createRadialGradient(cx, cy, 0, cx, cy, nucRadius * 1.5);
    grad.addColorStop(0, 'rgba(239, 68, 68, 0.25)');
    grad.addColorStop(1, 'rgba(239, 68, 68, 0)');
    ctx.fillStyle = grad;
    ctx.beginPath();
    ctx.arc(cx, cy, nucRadius * 1.5, 0, Math.PI * 2);
    ctx.fill();

    // Nucleus Core
    ctx.fillStyle = '#EF4444';
    ctx.beginPath();
    ctx.arc(cx, cy, nucRadius, 0, Math.PI * 2);
    ctx.fill();
    ctx.strokeStyle = '#DC2626';
    ctx.lineWidth = 2;
    ctx.stroke();

    // Nucleus text
    ctx.fillStyle = '#FFFFFF';
    ctx.font = 'bold ' + (nucRadius > 20 ? 11 : 9) + 'px ui-monospace, Menlo, monospace';
    ctx.textAlign = 'center';
    ctx.fillText('+' + el.z, cx, cy + (nucRadius > 20 ? 4 : 3));
    ctx.textAlign = 'left';

    // 3. Orbiting Electrons
    for (let s = 0; s < numShells; s++) {
      const r = baseR + s * stepR;
      const count = shells[s];
      const isValence = (s === numShells - 1);

      // Keplerian/Bohr angular velocity: inner faster, outer slower
      const omega = (1.5 / Math.pow(s + 1, 0.75)) * (s % 2 === 0 ? 1 : -1);
      const phase = simTime * omega;

      for (let e = 0; e < count; e++) {
        const ang = (e / count) * Math.PI * 2 + phase;
        const ex = cx + Math.cos(ang) * r;
        const ey = cy + Math.sin(ang) * r;

        // Electron circle
        const elecR = isValence ? 5.5 : 4.5;
        ctx.fillStyle = isValence ? '#D97706' : '#0284C7';
        ctx.beginPath();
        ctx.arc(ex, ey, elecR, 0, Math.PI * 2);
        ctx.fill();

        // Glow ring for valence electrons
        if (isValence) {
          ctx.strokeStyle = 'rgba(217, 119, 6, 0.4)';
          ctx.lineWidth = 4;
          ctx.beginPath();
          ctx.arc(ex, ey, elecR + 2, 0, Math.PI * 2);
          ctx.stroke();
        }

        ctx.strokeStyle = '#FFFFFF';
        ctx.lineWidth = 1.2;
        ctx.beginPath();
        ctx.arc(ex, ey, elecR, 0, Math.PI * 2);
        ctx.stroke();
      }
    }

    requestAnimationFrame(draw);
  }

  draw(performance.now());
})();
</script>
</body>
</html>
"""

    fun getPunnettGeneticsHtml(): String = """
<!DOCTYPE html>
<html lang="id">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; -webkit-tap-highlight-color: transparent; }
  html, body { position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; margin: 0; padding: 0; overflow: hidden; display: flex; flex-direction: column; background: #FFFFFF; color: #0F172A; user-select: none; -webkit-user-select: none; }
  #main-container { flex: 1 1 0%; min-height: 0; position: relative; width: 100%; background: #FFFFFF; overflow-y: auto; overflow-x: hidden; padding: 16px 20px; display: flex; flex-direction: column; gap: 14px; }
  .top-summary { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 12px; background: #F8FAFC; border: 1px solid #E2E8F0; border-radius: 12px; padding: 12px 18px; }
  .cross-title { font-size: 16px; font-weight: 800; color: #0F172A; display: flex; align-items: center; gap: 10px; }
  .badge-mode { background: #0284C7; color: #FFFFFF; font-size: 11px; font-weight: 700; padding: 4px 10px; border-radius: 6px; }
  .layout-grid { display: grid; grid-template-columns: 1fr 340px; gap: 16px; align-items: start; }
  @media (max-width: 860px) {
    .layout-grid { grid-template-columns: 1fr; }
  }
  .grid-card { background: #FFFFFF; border: 1px solid #E2E8F0; border-radius: 12px; padding: 16px; box-shadow: 0 2px 8px rgba(0,0,0,0.03); overflow-x: auto; }
  .stats-card { background: #F8FAFC; border: 1px solid #E2E8F0; border-radius: 12px; padding: 16px; display: flex; flex-direction: column; gap: 14px; }
  
  /* Punnett Table */
  table.punnett-table { border-collapse: separate; border-spacing: 6px; margin: 0 auto; }
  table.punnett-table th { background: #E0F2FE; color: #0369A1; font-weight: 700; font-size: 14px; padding: 8px 14px; border-radius: 8px; text-align: center; font-family: ui-monospace, Menlo, monospace; }
  table.punnett-table td { background: #FFFFFF; border: 2px solid #E2E8F0; border-radius: 10px; padding: 10px 14px; text-align: center; vertical-align: middle; transition: all 0.15s ease; min-width: 65px; }
  table.punnett-table td:hover { border-color: #0284C7; transform: scale(1.03); box-shadow: 0 4px 12px rgba(2, 132, 199, 0.15); }
  .cell-geno { font-size: 15px; font-weight: 800; color: #0F172A; font-family: ui-monospace, Menlo, monospace; }
  .cell-pheno { font-size: 10px; font-weight: 600; margin-top: 3px; display: block; border-radius: 4px; padding: 2px 4px; }
  
  /* Phenotype Tags */
  .pheno-dominant { background: #FEE2E2; color: #DC2626; }
  .pheno-recessive { background: #E0E7FF; color: #4338CA; }
  .pheno-ry { background: #FEF3C7; color: #B45309; }
  .pheno-rg { background: #DCFCE7; color: #15803D; }
  .pheno-wy { background: #FFEDD5; color: #C2410C; }
  .pheno-wg { background: #F3E8FF; color: #7E22CE; }

  /* Stat Bars */
  .ratio-group { display: flex; flex-direction: column; gap: 8px; }
  .ratio-title { font-size: 11px; font-weight: 700; color: #64748B; text-transform: uppercase; letter-spacing: 0.5px; }
  .bar-item { display: flex; flex-direction: column; gap: 3px; }
  .bar-header { display: flex; justify-content: space-between; font-size: 12px; font-weight: 600; }
  .bar-outer { width: 100%; height: 8px; background: #E2E8F0; border-radius: 4px; overflow: hidden; }
  .bar-inner { height: 100%; background: #0284C7; border-radius: 4px; }

  #control-dock { flex-shrink: 0; background: #F8FAFC; border-top: 1px solid #E2E8F0; padding: 12px 18px; display: flex; flex-direction: column; gap: 10px; z-index: 10; }
  .sliders-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap: 12px; }
  .control-group { display: flex; flex-direction: column; gap: 4px; }
  .control-label { display: flex; justify-content: space-between; font-size: 11px; font-weight: 600; color: #64748B; }
  .val-badge { font-weight: 700; color: #0284C7; font-family: ui-monospace, Menlo, monospace; }
  select { width: 100%; padding: 6px 10px; font-size: 12px; font-weight: 600; border-radius: 8px; border: 1px solid #CBD5E1; background: #FFFFFF; color: #0F172A; }
  .actions-row { display: flex; align-items: center; justify-content: space-between; gap: 10px; border-top: 1px solid #E2E8F0; padding-top: 8px; }
  .btn-group { display: flex; gap: 8px; }
  button { padding: 8px 16px; font-size: 12px; font-weight: 600; border-radius: 8px; cursor: pointer; border: 1px solid #CBD5E1; background: #FFFFFF; color: #0F172A; }
  button.active { background: #0F172A; color: #FFFFFF; border-color: #0F172A; }
  .formula-box { font-family: ui-monospace, Menlo, Consolas, monospace; font-size: 11px; color: #334155; background: #FFFFFF; border: 1px solid #E2E8F0; border-radius: 6px; padding: 6px 12px; }

  /* ── Studio Workbench Layout Upgrade ── */
  html, body {
    position: fixed !important;
    inset: 0 !important;
    width: 100vw !important;
    height: 100vh !important;
    margin: 0 !important;
    padding: 0 !important;
    overflow: hidden !important;
    display: flex !important;
    flex-direction: row !important;
    background: #FFFFFF !important;
    color: #0F172A !important;
    user-select: none !important;
    -webkit-user-select: none !important;
  }
  #canvas-container, #main-container {
    flex: 1 1 0% !important;
    min-width: 0 !important;
    height: 100% !important;
    position: relative !important;
    background: #FFFFFF !important;
    overflow: hidden !important;
  }
  #control-dock, #inspector-sidebar {
    width: 340px !important;
    max-width: 35vw !important;
    flex-shrink: 0 !important;
    height: 100% !important;
    background: #F8FAFC !important;
    border-left: 1px solid #E2E8F0 !important;
    border-top: none !important;
    padding: 20px 18px !important;
    display: flex !important;
    flex-direction: column !important;
    gap: 14px !important;
    overflow-y: auto !important;
    z-index: 10 !important;
    box-shadow: -4px 0 16px rgba(0,0,0,0.02) !important;
  }
  .sliders-grid {
    display: flex !important;
    flex-direction: column !important;
    gap: 12px !important;
  }
  .control-group {
    display: flex !important;
    flex-direction: column !important;
    gap: 6px !important;
    background: #FFFFFF !important;
    border: 1px solid #E2E8F0 !important;
    border-radius: 10px !important;
    padding: 10px 12px !important;
    box-shadow: 0 1px 3px rgba(0,0,0,0.02) !important;
  }
  .actions-row {
    display: flex !important;
    flex-direction: column !important;
    gap: 10px !important;
    border-top: 1px solid #E2E8F0 !important;
    padding-top: 14px !important;
    margin-top: auto !important;
  }
  .btn-group {
    display: flex !important;
    gap: 8px !important;
    width: 100% !important;
  }
  .btn-group button {
    flex: 1 !important;
  }
  .formula-box, .theory-card {
    font-family: ui-monospace, Menlo, Consolas, monospace !important;
    font-size: 11px !important;
    color: #0369A1 !important;
    background: #EFF6FF !important;
    border: 1px solid #BFDBFE !important;
    border-radius: 8px !important;
    padding: 10px 12px !important;
    line-height: 1.5 !important;
    word-break: break-word !important;
  }


  .layout-grid {
    display: flex !important;
    flex-direction: column !important;
    gap: 14px !important;
  }
        
</style>
</head>
<body>
<div id="main-container">
  <div class="top-summary">
    <div class="cross-title">
      <span class="badge-mode" id="modeBadge">MONOHIBRID</span>
      <span id="titleParents">Induk: Mm (Merah) × Mm (Merah)</span>
    </div>
    <div style="font-size: 12px; color: #64748B; font-weight: 600;" id="subtitleLaw">
      Hukum I Mendel: Segregasi Bebas Alel
    </div>
  </div>

  <div class="layout-grid">
    <div class="grid-card">
      <div id="tableContainer"></div>
    </div>
    <div class="stats-card">
      <div class="ratio-group">
        <div class="ratio-title">Rasio Fenotipe Anakan (F₂)</div>
        <div id="phenoBars"></div>
      </div>
      <div class="ratio-group" style="border-top: 1px solid #E2E8F0; padding-top: 10px;">
        <div class="ratio-title">Rasio Genotipe Anakan (F₂)</div>
        <div id="genoBars"></div>
      </div>
    </div>
  </div>
</div>

<div id="control-dock">
  <div class="sliders-grid">
    <div class="control-group">
      <div class="control-label"><span>Jenis Persilangan</span><span class="val-badge" id="valModeBadge">1 Pasang Sifat</span></div>
      <select id="selectMode">
        <option value="mono" selected>Monohibrid (1 Sifat: Mm)</option>
        <option value="di">Dihibrid (2 Sifat: BbKk)</option>
      </select>
    </div>
    <div class="control-group">
      <div class="control-label"><span>Genotipe Induk 1 (P₁)</span><span class="val-badge" id="valP1">Mm</span></div>
      <select id="selectP1"></select>
    </div>
    <div class="control-group">
      <div class="control-label"><span>Genotipe Induk 2 (P₂)</span><span class="val-badge" id="valP2">Mm</span></div>
      <select id="selectP2"></select>
    </div>
    <div class="control-group">
      <div class="control-label"><span>Contoh Klasik</span><span class="val-badge">Preset</span></div>
      <div class="btn-group">
        <button id="btnClassic" style="flex:1;">Heterozigot (Aa × Aa)</button>
      </div>
    </div>
  </div>
  <div class="actions-row">
    <div class="btn-group">
      <button id="btnReset">Reset Default</button>
    </div>
    <div class="formula-box" id="formulaPill">
      Monohibrid: 1 MM : 2 Mm : 1 mm (Fenotipe 3 Merah : 1 Putih)
    </div>
  </div>
</div>

<script>
(function() {
  const MONO_OPTIONS = [
    { code: 'MM', name: 'MM (Homozigot Dominan)' },
    { code: 'Mm', name: 'Mm (Heterozigot)' },
    { code: 'mm', name: 'mm (Homozigot Resesif)' }
  ];

  const DI_OPTIONS = [
    { code: 'BBKK', name: 'BBKK (Bulat Kuning)' },
    { code: 'BbKk', name: 'BbKk (Bulat Kuning)' },
    { code: 'BBkk', name: 'BBkk (Bulat Hijau)' },
    { code: 'Bbkk', name: 'Bbkk (Bulat Hijau)' },
    { code: 'bbKK', name: 'bbKK (Keriput Kuning)' },
    { code: 'bbKk', name: 'bbKk (Keriput Kuning)' },
    { code: 'bbkk', name: 'bbkk (Keriput Hijau)' }
  ];

  let currentMode = 'mono';
  let p1Geno = 'Mm';
  let p2Geno = 'Mm';

  const selectMode = document.getElementById('selectMode');
  const selectP1 = document.getElementById('selectP1');
  const selectP2 = document.getElementById('selectP2');
  const modeBadge = document.getElementById('modeBadge');
  const titleParents = document.getElementById('titleParents');
  const subtitleLaw = document.getElementById('subtitleLaw');
  const tableContainer = document.getElementById('tableContainer');
  const phenoBars = document.getElementById('phenoBars');
  const genoBars = document.getElementById('genoBars');
  const formulaPill = document.getElementById('formulaPill');
  const valModeBadge = document.getElementById('valModeBadge');
  const valP1 = document.getElementById('valP1');
  const valP2 = document.getElementById('valP2');
  const btnClassic = document.getElementById('btnClassic');
  const btnReset = document.getElementById('btnReset');

  function populateParentSelects() {
    const list = currentMode === 'mono' ? MONO_OPTIONS : DI_OPTIONS;
    selectP1.innerHTML = '';
    selectP2.innerHTML = '';
    for (let i = 0; i < list.length; i++) {
      const opt1 = document.createElement('option');
      opt1.value = list[i].code;
      opt1.textContent = list[i].name;
      selectP1.appendChild(opt1);

      const opt2 = document.createElement('option');
      opt2.value = list[i].code;
      opt2.textContent = list[i].name;
      selectP2.appendChild(opt2);
    }
    selectP1.value = p1Geno;
    selectP2.value = p2Geno;
  }

  function getGametes(geno) {
    if (currentMode === 'mono') {
      return [geno[0], geno[1]];
    } else {
      // Dihybrid: AaBb -> AB, Ab, aB, ab
      const a = [geno[0], geno[1]];
      const b = [geno[2], geno[3]];
      const out = [];
      for (let i = 0; i < 2; i++) {
        for (let j = 0; j < 2; j++) {
          out.push(a[i] + b[j]);
        }
      }
      return out;
    }
  }

  function combineGametes(g1, g2) {
    if (currentMode === 'mono') {
      const alleles = [g1, g2].sort();
      // Dominant capital first
      if (alleles[0] === alleles[0].toLowerCase() && alleles[1] === alleles[1].toUpperCase()) {
        return alleles[1] + alleles[0];
      }
      return alleles[0] === alleles[0].toUpperCase() ? alleles[0] + alleles[1] : alleles[1] + alleles[0];
    } else {
      // Dihybrid: A1B1 + A2B2
      const a1 = g1[0], b1 = g1[1];
      const a2 = g2[0], b2 = g2[1];
      const pairA = (a1.toUpperCase() === a1 ? a1 + a2 : (a2.toUpperCase() === a2 ? a2 + a1 : a1 + a2));
      const pairB = (b1.toUpperCase() === b1 ? b1 + b2 : (b2.toUpperCase() === b2 ? b2 + b1 : b1 + b2));
      return pairA + pairB;
    }
  }

  function getPhenotype(childGeno) {
    if (currentMode === 'mono') {
      if (childGeno.indexOf('M') !== -1) {
        return { name: 'Bunga Merah', class: 'pheno-dominant', color: '#DC2626' };
      } else {
        return { name: 'Bunga Putih', class: 'pheno-recessive', color: '#4338CA' };
      }
    } else {
      // B = Bulat, b = Keriput; K = Kuning, k = Hijau
      const isBulat = childGeno.indexOf('B') !== -1;
      const isKuning = childGeno.indexOf('K') !== -1;
      if (isBulat && isKuning) {
        return { name: 'Bulat Kuning', class: 'pheno-ry', color: '#B45309' };
      } else if (isBulat && !isKuning) {
        return { name: 'Bulat Hijau', class: 'pheno-rg', color: '#15803D' };
      } else if (!isBulat && isKuning) {
        return { name: 'Keriput Kuning', class: 'pheno-wy', color: '#C2410C' };
      } else {
        return { name: 'Keriput Hijau', class: 'pheno-wg', color: '#7E22CE' };
      }
    }
  }

  function render() {
    valP1.textContent = p1Geno;
    valP2.textContent = p2Geno;
    modeBadge.textContent = currentMode === 'mono' ? 'MONOHIBRID' : 'DIHIBRID';
    valModeBadge.textContent = currentMode === 'mono' ? '1 Pasang Sifat' : '2 Pasang Sifat';
    subtitleLaw.textContent = currentMode === 'mono' ? 'Hukum I Mendel: Segregasi Bebas Alel' : 'Hukum II Mendel: Asortasi Bebas Gen';

    const p1Pheno = getPhenotype(p1Geno).name;
    const p2Pheno = getPhenotype(p2Geno).name;
    titleParents.textContent = 'Induk: ' + p1Geno + ' (' + p1Pheno + ') × ' + p2Geno + ' (' + p2Pheno + ')';

    const gam1 = getGametes(p1Geno);
    const gam2 = getGametes(p2Geno);

    // Build Table
    let tableHtml = '<table class="punnett-table">';
    tableHtml += '<tr><th style="background:#F1F5F9; color:#64748B;">♀ \\ ♂</th>';
    for (let j = 0; j < gam2.length; j++) {
      tableHtml += '<th>' + gam2[j] + '</th>';
    }
    tableHtml += '</tr>';

    const genoCounts = {};
    const phenoCounts = {};
    const totalOffspring = gam1.length * gam2.length;

    for (let i = 0; i < gam1.length; i++) {
      tableHtml += '<tr>';
      tableHtml += '<th>' + gam1[i] + '</th>';
      for (let j = 0; j < gam2.length; j++) {
        const child = combineGametes(gam1[i], gam2[j]);
        const ph = getPhenotype(child);
        genoCounts[child] = (genoCounts[child] || 0) + 1;
        phenoCounts[ph.name] = (phenoCounts[ph.name] || 0) + 1;

        tableHtml += '<td>';
        tableHtml += '<div class="cell-geno">' + child + '</div>';
        tableHtml += '<span class="cell-pheno ' + ph.class + '">' + ph.name + '</span>';
        tableHtml += '</td>';
      }
      tableHtml += '</tr>';
    }
    tableHtml += '</table>';
    tableContainer.innerHTML = tableHtml;

    // Render Phenotype Bars
    let phenoHtml = '';
    const phenoEntries = Object.keys(phenoCounts);
    for (let i = 0; i < phenoEntries.length; i++) {
      const name = phenoEntries[i];
      const count = phenoCounts[name];
      const pct = (count / totalOffspring * 100).toFixed(1);
      phenoHtml += '<div class="bar-item">';
      phenoHtml += '  <div class="bar-header"><span>' + name + ' (' + count + '/' + totalOffspring + ')</span><span style="font-family:ui-monospace,Menlo,monospace; font-weight:700;">' + pct + '%</span></div>';
      phenoHtml += '  <div class="bar-outer"><div class="bar-inner" style="width:' + pct + '%; background: #0284C7;"></div></div>';
      phenoHtml += '</div>';
    }
    phenoBars.innerHTML = phenoHtml;

    // Render Genotype Bars
    let genoHtml = '';
    const genoEntries = Object.keys(genoCounts);
    for (let i = 0; i < genoEntries.length; i++) {
      const code = genoEntries[i];
      const count = genoCounts[code];
      const pct = (count / totalOffspring * 100).toFixed(1);
      genoHtml += '<div class="bar-item">';
      genoHtml += '  <div class="bar-header"><span style="font-family:ui-monospace,Menlo,monospace; font-weight:700;">' + code + ' (' + count + '/' + totalOffspring + ')</span><span>' + pct + '%</span></div>';
      genoHtml += '  <div class="bar-outer"><div class="bar-inner" style="width:' + pct + '%; background: #64748B;"></div></div>';
      genoHtml += '</div>';
    }
    genoBars.innerHTML = genoHtml;

    // Formula summary pill
    if (currentMode === 'mono') {
      formulaPill.textContent = 'Rasio Fenotipe F₂: ' + phenoEntries.map((k) => k + ' ' + phenoCounts[k]).join(' : ') + ' (Total ' + totalOffspring + ' Anakan)';
    } else {
      formulaPill.textContent = 'Rasio Dihibrid Klasik: 9 Bulat-Kuning : 3 Bulat-Hijau : 3 Keriput-Kuning : 1 Keriput-Hijau';
    }
  }

  selectMode.onchange = (e) => {
    currentMode = e.target.value;
    if (currentMode === 'mono') {
      p1Geno = 'Mm';
      p2Geno = 'Mm';
    } else {
      p1Geno = 'BbKk';
      p2Geno = 'BbKk';
    }
    populateParentSelects();
    render();
  };

  selectP1.onchange = (e) => { p1Geno = e.target.value; render(); };
  selectP2.onchange = (e) => { p2Geno = e.target.value; render(); };

  btnClassic.onclick = () => {
    if (currentMode === 'mono') {
      p1Geno = 'Mm'; p2Geno = 'Mm';
    } else {
      p1Geno = 'BbKk'; p2Geno = 'BbKk';
    }
    selectP1.value = p1Geno;
    selectP2.value = p2Geno;
    render();
  };

  btnReset.onclick = () => {
    currentMode = 'mono';
    selectMode.value = 'mono';
    p1Geno = 'Mm';
    p2Geno = 'Mm';
    populateParentSelects();
    render();
  };

  populateParentSelects();
  render();
})();
</script>
</body>
</html>
"""

    fun getRiemannSumSimulationHtml(): String = """
﻿<!DOCTYPE html>
<html lang="id">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; -webkit-tap-highlight-color: transparent; }
  html, body { width: 100%; height: 100%; overflow: hidden; display: flex; flex-direction: column; background: #FFFFFF; color: #0F172A; user-select: none; -webkit-user-select: none; }
  #canvas-container { flex: 1 1 0%; min-height: 0; position: relative; width: 100%; background: #FFFFFF; overflow: hidden; }
  canvas { position: absolute; top: 0; left: 0; width: 100%; height: 100%; display: block; touch-action: none; }
  #glass-hud { position: absolute; top: 14px; left: 14px; background: rgba(255, 255, 255, 0.94); backdrop-filter: blur(8px); border: 1px solid #E2E8F0; border-radius: 12px; padding: 10px 14px; box-shadow: 0 4px 14px rgba(0,0,0,0.06); font-size: 12px; line-height: 1.6; max-width: 340px; pointer-events: none; z-index: 5; }
  .hud-row { display: flex; justify-content: space-between; gap: 12px; }
  .hud-val { font-weight: 700; color: #7C3AED; font-family: ui-monospace, Menlo, Consolas, monospace; }
  #control-dock { flex-shrink: 0; background: #F8FAFC; border-top: 1px solid #E2E8F0; padding: 12px 18px; display: flex; flex-direction: column; gap: 10px; z-index: 10; }
  .sliders-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 12px; }
  .control-group { display: flex; flex-direction: column; gap: 4px; }
  .control-label { display: flex; justify-content: space-between; font-size: 11px; font-weight: 600; color: #64748B; }
  .val-badge { font-weight: 700; color: #7C3AED; font-family: ui-monospace, Menlo, monospace; }
  input[type="range"] { width: 100%; height: 26px; accent-color: #7C3AED; cursor: pointer; }
  select { background: #FFFFFF; border: 1px solid #CBD5E1; border-radius: 6px; padding: 6px 8px; font-size: 12px; font-weight: 600; color: #0F172A; }
  .actions-row { display: flex; align-items: center; justify-content: space-between; gap: 10px; border-top: 1px solid #E2E8F0; padding-top: 8px; }
  .formula-box { font-family: ui-monospace, Menlo, Consolas, monospace; font-size: 11px; color: #334155; background: #FFFFFF; border: 1px solid #E2E8F0; border-radius: 6px; padding: 6px 12px; }
  .badge-part { display: inline-block; padding: 2px 8px; background: #EDE9FE; color: #6D28D9; border-radius: 4px; font-weight: 600; font-size: 11px; }

  /* ── Studio Workbench Layout Upgrade ── */
  html, body {
    position: fixed !important;
    inset: 0 !important;
    width: 100vw !important;
    height: 100vh !important;
    margin: 0 !important;
    padding: 0 !important;
    overflow: hidden !important;
    display: flex !important;
    flex-direction: row !important;
    background: #FFFFFF !important;
    color: #0F172A !important;
    user-select: none !important;
    -webkit-user-select: none !important;
  }
  #canvas-container, #main-container {
    flex: 1 1 0% !important;
    min-width: 0 !important;
    height: 100% !important;
    position: relative !important;
    background: #FFFFFF !important;
    overflow: hidden !important;
  }
  #control-dock, #inspector-sidebar {
    width: 340px !important;
    max-width: 35vw !important;
    flex-shrink: 0 !important;
    height: 100% !important;
    background: #F8FAFC !important;
    border-left: 1px solid #E2E8F0 !important;
    border-top: none !important;
    padding: 20px 18px !important;
    display: flex !important;
    flex-direction: column !important;
    gap: 14px !important;
    overflow-y: auto !important;
    z-index: 10 !important;
    box-shadow: -4px 0 16px rgba(0,0,0,0.02) !important;
  }
  .sliders-grid {
    display: flex !important;
    flex-direction: column !important;
    gap: 12px !important;
  }
  .control-group {
    display: flex !important;
    flex-direction: column !important;
    gap: 6px !important;
    background: #FFFFFF !important;
    border: 1px solid #E2E8F0 !important;
    border-radius: 10px !important;
    padding: 10px 12px !important;
    box-shadow: 0 1px 3px rgba(0,0,0,0.02) !important;
  }
  .actions-row {
    display: flex !important;
    flex-direction: column !important;
    gap: 10px !important;
    border-top: 1px solid #E2E8F0 !important;
    padding-top: 14px !important;
    margin-top: auto !important;
  }
  .btn-group {
    display: flex !important;
    gap: 8px !important;
    width: 100% !important;
  }
  .btn-group button {
    flex: 1 !important;
  }
  .formula-box, .theory-card {
    font-family: ui-monospace, Menlo, Consolas, monospace !important;
    font-size: 11px !important;
    color: #0369A1 !important;
    background: #EFF6FF !important;
    border: 1px solid #BFDBFE !important;
    border-radius: 8px !important;
    padding: 10px 12px !important;
    line-height: 1.5 !important;
    word-break: break-word !important;
  }

</style>
</head>
<body>
<div id="canvas-container">
  <canvas id="simCanvas"></canvas>
  <div id="glass-hud">
    <div class="hud-row"><span>Lebar Partisi (Δx):</span><span id="hudDx" class="hud-val">0.250</span></div>
    <div class="hud-row"><span>Luas Aproksimasi (S_N):</span><span id="hudApprox" class="hud-val">3.000</span></div>
    <div class="hud-row"><span>Luas Eksak (∫ f(x)dx):</span><span id="hudExact" class="hud-val">3.000</span></div>
    <div class="hud-row"><span>Galat / Error Absolut:</span><span id="hudError" class="hud-val">0.00%</span></div>
  </div>
</div>
<div id="control-dock">
  <div class="sliders-grid">
    <div class="control-group">
      <div class="control-label"><span>Fungsi Matematika</span></div>
      <select id="selectFunc">
        <option value="parab" selected>f(x) = x²</option>
        <option value="cubic">f(x) = x³ - x</option>
        <option value="sine">f(x) = sin(x)</option>
        <option value="bell">f(x) = e^(-x²)</option>
      </select>
    </div>
    <div class="control-group">
      <div class="control-label"><span>Metode Partisi</span></div>
      <select id="selectRule">
        <option value="left" selected>Jumlah Riemann Kiri (Left)</option>
        <option value="right">Jumlah Riemann Kanan (Right)</option>
        <option value="midpoint">Titik Tengah (Midpoint)</option>
        <option value="trapezoid">Aturan Trapesium (Trapezoid)</option>
      </select>
    </div>
    <div class="control-group">
      <div class="control-label"><span>Jumlah Partisi (N)</span><span class="val-badge" id="valN">12</span></div>
      <input type="range" id="rangeN" min="2" max="64" step="1" value="12">
    </div>
    <div class="control-group">
      <div class="control-label"><span>Batas Integrasi [a, b]</span><span class="val-badge" id="valBounds">[-1.0, 2.0]</span></div>
      <div style="font-size: 11px; color: #64748B; padding-top: 4px;">*Geser titik pegangan bulat a & b di sumbu X</div>
    </div>
  </div>
  <div class="actions-row">
    <span class="badge-part">Kalkulus Integral Riemann</span>
    <div class="formula-box" id="formulaPill">
      lim (N→∞) Σ f(x_i) Δx = ∫_a^b f(x) dx
    </div>
  </div>
</div>

<script>
(function() {
  const container = document.getElementById('canvas-container');
  const canvas = document.getElementById('simCanvas');
  const ctx = canvas.getContext('2d');

  let width = 600, height = 400;
  function resize() {
    const w = container.clientWidth || 600;
    const h = container.clientHeight || 400;
    const dpr = window.devicePixelRatio || 1;
    canvas.width = Math.round(w * dpr);
    canvas.height = Math.round(h * dpr);
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    width = w;
    height = h;
  }
  window.addEventListener('resize', resize);
  if (window.ResizeObserver) {
    new ResizeObserver(resize).observe(container);
  }
  resize();

  const FUNCS = {
    parab: {
      label: 'f(x) = x²',
      f: (x) => x * x,
      integral: (a, b) => (b ** 3 - a ** 3) / 3
    },
    cubic: {
      label: 'f(x) = x³ - x',
      f: (x) => x * x * x - x,
      integral: (a, b) => (b ** 4 - a ** 4) / 4 - (b * b - a * a) / 2
    },
    sine: {
      label: 'f(x) = sin(x)',
      f: (x) => Math.sin(x),
      integral: (a, b) => -Math.cos(b) + Math.cos(a)
    },
    bell: {
      label: 'f(x) = e^(-x²)',
      f: (x) => Math.exp(-x * x),
      integral: null // Gaussian numerical fallback
    }
  };

  let funcKey = 'parab';
  let rule = 'left';
  let N = 12;
  let boundA = -1.0;
  let boundB = 2.0;

  const selectFunc = document.getElementById('selectFunc');
  const selectRule = document.getElementById('selectRule');
  const rangeN = document.getElementById('rangeN');
  const valN = document.getElementById('valN');
  const valBounds = document.getElementById('valBounds');
  const hudDx = document.getElementById('hudDx');
  const hudApprox = document.getElementById('hudApprox');
  const hudExact = document.getElementById('hudExact');
  const hudError = document.getElementById('hudError');
  const formulaPill = document.getElementById('formulaPill');

  const X_MIN = -3, X_MAX = 3, Y_MIN = -2, Y_MAX = 8;
  function w2sX(x) { return 50 + ((x - X_MIN) / (X_MAX - X_MIN)) * (width - 80); }
  function w2sY(y) { return 30 + ((Y_MAX - y) / (Y_MAX - Y_MIN)) * (height - 70); }
  function s2wX(sx) { return X_MIN + ((sx - 50) / (width - 80)) * (X_MAX - X_MIN); }

  function computeIntegral(a, b) {
    const fObj = FUNCS[funcKey];
    if (fObj.integral) return fObj.integral(a, b);
    // Simpson's rule for Gaussian bell curve
    const steps = 1000;
    const h = (b - a) / steps;
    let sum = fObj.f(a) + fObj.f(b);
    for (let i = 1; i < steps; i++) {
      const x = a + i * h;
      sum += (i % 2 === 0 ? 2 : 4) * fObj.f(x);
    }
    return (sum * h) / 3;
  }

  function computeRiemannSum() {
    const f = FUNCS[funcKey].f;
    const dx = (boundB - boundA) / N;
    let s = 0;
    for (let i = 0; i < N; i++) {
      const xi = boundA + i * dx;
      let y;
      if (rule === 'left') y = f(xi);
      else if (rule === 'right') y = f(xi + dx);
      else if (rule === 'midpoint') y = f(xi + dx / 2);
      else y = (f(xi) + f(xi + dx)) / 2; // trapezoid
      s += y * dx;
    }
    return s;
  }

  function updateHUD() {
    const dx = (boundB - boundA) / N;
    const approx = computeRiemannSum();
    const exact = computeIntegral(boundA, boundB);
    const err = Math.abs(approx - exact);
    const errPct = exact !== 0 ? (err / Math.abs(exact)) * 100 : 0;

    hudDx.textContent = dx.toFixed(3);
    hudApprox.textContent = approx.toFixed(4);
    hudExact.textContent = exact.toFixed(4);
    hudError.textContent = err.toFixed(4) + ' (' + errPct.toFixed(2) + '%)';
    valBounds.textContent = '[' + boundA.toFixed(1) + ', ' + boundB.toFixed(1) + ']';
    formulaPill.textContent = FUNCS[funcKey].label + ' pada [' + boundA.toFixed(1) + ', ' + boundB.toFixed(1) + '] | N = ' + N;
  }

  selectFunc.onchange = (e) => { funcKey = e.target.value; updateHUD(); };
  selectRule.onchange = (e) => { rule = e.target.value; updateHUD(); };
  rangeN.oninput = (e) => {
    N = parseInt(e.target.value);
    valN.textContent = N;
    updateHUD();
  };

  // Draggable Bound Handles directly on X-axis
  let draggingHandle = null; // 'a' or 'b'
  function getPos(e) {
    const rect = canvas.getBoundingClientRect();
    const cx = e.touches ? e.touches[0].clientX : e.clientX;
    const cy = e.touches ? e.touches[0].clientY : e.clientY;
    return {
      x: (cx - rect.left) * (canvas.width / rect.width) / (window.devicePixelRatio || 1),
      y: (cy - rect.top) * (canvas.height / rect.height) / (window.devicePixelRatio || 1)
    };
  }

  canvas.addEventListener('pointerdown', (e) => {
    const p = getPos(e);
    const ax = w2sX(boundA), ay = w2sY(0);
    const bx = w2sX(boundB), by = w2sY(0);

    if (Math.hypot(p.x - ax, p.y - ay) < 25) {
      draggingHandle = 'a';
      canvas.setPointerCapture(e.pointerId);
    } else if (Math.hypot(p.x - bx, p.y - by) < 25) {
      draggingHandle = 'b';
      canvas.setPointerCapture(e.pointerId);
    }
  });

  canvas.addEventListener('pointermove', (e) => {
    if (!draggingHandle) return;
    const p = getPos(e);
    const wx = Math.round(s2wX(p.x) * 10) / 10;
    if (draggingHandle === 'a') {
      boundA = Math.max(-2.5, Math.min(boundB - 0.4, wx));
    } else {
      boundB = Math.max(boundA + 0.4, Math.min(2.5, wx));
    }
    updateHUD();
  });

  function stopDrag() { draggingHandle = null; }
  canvas.addEventListener('pointerup', stopDrag);
  canvas.addEventListener('pointercancel', stopDrag);

  // Draw loop
  function draw() {
    ctx.clearRect(0, 0, width, height);

    // Subtle background grid dots
    ctx.fillStyle = 'rgba(0, 0, 0, 0.04)';
    for (let x = 20; x < width; x += 24) {
      for (let y = 20; y < height; y += 24) {
        ctx.fillRect(x, y, 2, 2);
      }
    }

    // Gridlines & Axes
    ctx.strokeStyle = '#F1F5F9';
    ctx.lineWidth = 1;
    for (let x = X_MIN; x <= X_MAX; x++) {
      ctx.beginPath(); ctx.moveTo(w2sX(x), 30); ctx.lineTo(w2sX(x), height - 40); ctx.stroke();
    }
    for (let y = Y_MIN; y <= Y_MAX; y += 2) {
      ctx.beginPath(); ctx.moveTo(50, w2sY(y)); ctx.lineTo(width - 30, w2sY(y)); ctx.stroke();
    }

    // Main Axes
    ctx.strokeStyle = '#94A3B8';
    ctx.lineWidth = 1.5;
    ctx.beginPath();
    ctx.moveTo(50, w2sY(0)); ctx.lineTo(width - 30, w2sY(0)); // X axis
    ctx.moveTo(w2sX(0), 30); ctx.lineTo(w2sX(0), height - 40); // Y axis
    ctx.stroke();

    // Axis Labels
    ctx.fillStyle = '#64748B';
    ctx.font = '10px ui-monospace, Menlo, monospace';
    ctx.textAlign = 'center';
    for (let x = X_MIN; x <= X_MAX; x++) {
      if (x !== 0) ctx.fillText(x, w2sX(x), w2sY(0) + 14);
    }
    ctx.textAlign = 'right';
    for (let y = Y_MIN; y <= Y_MAX; y += 2) {
      if (y !== 0) ctx.fillText(y, 44, w2sY(y) + 4);
    }

    // Draw Riemann Partitions
    const f = FUNCS[funcKey].f;
    const dx = (boundB - boundA) / N;
    const yBase = w2sY(0);

    for (let i = 0; i < N; i++) {
      const xi = boundA + i * dx;
      const x1 = w2sX(xi);
      const x2 = w2sX(xi + dx);

      if (rule === 'trapezoid') {
        const yL = w2sY(f(xi));
        const yR = w2sY(f(xi + dx));
        ctx.fillStyle = 'rgba(124, 58, 237, 0.22)';
        ctx.beginPath();
        ctx.moveTo(x1, yBase);
        ctx.lineTo(x1, yL);
        ctx.lineTo(x2, yR);
        ctx.lineTo(x2, yBase);
        ctx.closePath();
        ctx.fill();

        ctx.strokeStyle = '#7C3AED';
        ctx.lineWidth = 1.2;
        ctx.beginPath();
        ctx.moveTo(x1, yL); ctx.lineTo(x2, yR);
        ctx.stroke();
      } else {
        let sampleY;
        if (rule === 'left') sampleY = f(xi);
        else if (rule === 'right') sampleY = f(xi + dx);
        else sampleY = f(xi + dx / 2);

        const yTop = w2sY(sampleY);
        const rectY = Math.min(yTop, yBase);
        const rectH = Math.abs(yBase - yTop);

        ctx.fillStyle = sampleY >= 0 ? 'rgba(124, 58, 237, 0.22)' : 'rgba(239, 68, 68, 0.22)';
        ctx.fillRect(x1, rectY, x2 - x1, rectH);

        ctx.strokeStyle = sampleY >= 0 ? '#7C3AED' : '#EF4444';
        ctx.lineWidth = 1;
        ctx.strokeRect(x1, rectY, x2 - x1, rectH);
      }
    }

    // Continuous Function Curve
    ctx.strokeStyle = '#0284C7';
    ctx.lineWidth = 2.5;
    ctx.beginPath();
    let started = false;
    for (let i = 0; i <= 300; i++) {
      const x = X_MIN + (i / 300) * (X_MAX - X_MIN);
      const y = f(x);
      if (!Number.isFinite(y)) { started = false; continue; }
      const sx = w2sX(x);
      const sy = w2sY(y);
      if (!started) { ctx.moveTo(sx, sy); started = true; }
      else { ctx.lineTo(sx, sy); }
    }
    ctx.stroke();

    // Draggable Bound Markers a and b
    function drawBoundHandle(xVal, label, color) {
      const sx = w2sX(xVal);
      const sy = w2sY(0);

      // Vertical hairline
      ctx.strokeStyle = color;
      ctx.lineWidth = 1.5;
      ctx.setLineDash([4, 4]);
      ctx.beginPath();
      ctx.moveTo(sx, 30);
      ctx.lineTo(sx, height - 40);
      ctx.stroke();
      ctx.setLineDash([]);

      // Circular Handle
      ctx.fillStyle = color;
      ctx.beginPath();
      ctx.arc(sx, sy, 8, 0, Math.PI * 2);
      ctx.fill();
      ctx.fillStyle = '#FFFFFF';
      ctx.beginPath();
      ctx.arc(sx, sy, 4, 0, Math.PI * 2);
      ctx.fill();

      // Label
      ctx.fillStyle = color;
      ctx.font = 'bold 12px ui-monospace, Menlo, monospace';
      ctx.textAlign = 'center';
      ctx.fillText(label + ' = ' + xVal.toFixed(1), sx, sy + 22);
    }

    drawBoundHandle(boundA, 'a', '#D97706');
    drawBoundHandle(boundB, 'b', '#059669');

    requestAnimationFrame(draw);
  }
  updateHUD();
  requestAnimationFrame(draw);
})();
</script>
</body>
</html>
"""

    fun getCarnotCycleSimulationHtml(): String = """
﻿<!DOCTYPE html>
<html lang="id">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; -webkit-tap-highlight-color: transparent; }
  html, body { width: 100%; height: 100%; overflow: hidden; display: flex; flex-direction: column; background: #FFFFFF; color: #0F172A; user-select: none; -webkit-user-select: none; }
  #canvas-container { flex: 1 1 0%; min-height: 0; position: relative; width: 100%; background: #FFFFFF; overflow: hidden; }
  canvas { position: absolute; top: 0; left: 0; width: 100%; height: 100%; display: block; touch-action: none; }
  #glass-hud { position: absolute; top: 14px; left: 14px; background: rgba(255, 255, 255, 0.94); backdrop-filter: blur(8px); border: 1px solid #E2E8F0; border-radius: 12px; padding: 10px 14px; box-shadow: 0 4px 14px rgba(0,0,0,0.06); font-size: 12px; line-height: 1.6; max-width: 330px; pointer-events: none; z-index: 5; }
  .hud-row { display: flex; justify-content: space-between; gap: 12px; }
  .hud-val { font-weight: 700; color: #D97706; font-family: ui-monospace, Menlo, Consolas, monospace; }
  #control-dock { flex-shrink: 0; background: #F8FAFC; border-top: 1px solid #E2E8F0; padding: 12px 18px; display: flex; flex-direction: column; gap: 10px; z-index: 10; }
  .sliders-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap: 12px; }
  .control-group { display: flex; flex-direction: column; gap: 4px; }
  .control-label { display: flex; justify-content: space-between; font-size: 11px; font-weight: 600; color: #64748B; }
  .val-badge { font-weight: 700; color: #D97706; font-family: ui-monospace, Menlo, monospace; }
  input[type="range"] { width: 100%; height: 26px; accent-color: #D97706; cursor: pointer; }
  .actions-row { display: flex; align-items: center; justify-content: space-between; gap: 10px; border-top: 1px solid #E2E8F0; padding-top: 8px; }
  .btn-group { display: flex; gap: 8px; }
  button { padding: 8px 16px; font-size: 12px; font-weight: 600; border-radius: 8px; cursor: pointer; border: 1px solid #CBD5E1; background: #FFFFFF; color: #0F172A; }
  button.active { background: #0F172A; color: #FFFFFF; border-color: #0F172A; }
  .formula-box { font-family: ui-monospace, Menlo, Consolas, monospace; font-size: 11px; color: #334155; background: #FFFFFF; border: 1px solid #E2E8F0; border-radius: 6px; padding: 6px 12px; }

  /* ── Studio Workbench Layout Upgrade ── */
  html, body {
    position: fixed !important;
    inset: 0 !important;
    width: 100vw !important;
    height: 100vh !important;
    margin: 0 !important;
    padding: 0 !important;
    overflow: hidden !important;
    display: flex !important;
    flex-direction: row !important;
    background: #FFFFFF !important;
    color: #0F172A !important;
    user-select: none !important;
    -webkit-user-select: none !important;
  }
  #canvas-container, #main-container {
    flex: 1 1 0% !important;
    min-width: 0 !important;
    height: 100% !important;
    position: relative !important;
    background: #FFFFFF !important;
    overflow: hidden !important;
  }
  #control-dock, #inspector-sidebar {
    width: 340px !important;
    max-width: 35vw !important;
    flex-shrink: 0 !important;
    height: 100% !important;
    background: #F8FAFC !important;
    border-left: 1px solid #E2E8F0 !important;
    border-top: none !important;
    padding: 20px 18px !important;
    display: flex !important;
    flex-direction: column !important;
    gap: 14px !important;
    overflow-y: auto !important;
    z-index: 10 !important;
    box-shadow: -4px 0 16px rgba(0,0,0,0.02) !important;
  }
  .sliders-grid {
    display: flex !important;
    flex-direction: column !important;
    gap: 12px !important;
  }
  .control-group {
    display: flex !important;
    flex-direction: column !important;
    gap: 6px !important;
    background: #FFFFFF !important;
    border: 1px solid #E2E8F0 !important;
    border-radius: 10px !important;
    padding: 10px 12px !important;
    box-shadow: 0 1px 3px rgba(0,0,0,0.02) !important;
  }
  .actions-row {
    display: flex !important;
    flex-direction: column !important;
    gap: 10px !important;
    border-top: 1px solid #E2E8F0 !important;
    padding-top: 14px !important;
    margin-top: auto !important;
  }
  .btn-group {
    display: flex !important;
    gap: 8px !important;
    width: 100% !important;
  }
  .btn-group button {
    flex: 1 !important;
  }
  .formula-box, .theory-card {
    font-family: ui-monospace, Menlo, Consolas, monospace !important;
    font-size: 11px !important;
    color: #0369A1 !important;
    background: #EFF6FF !important;
    border: 1px solid #BFDBFE !important;
    border-radius: 8px !important;
    padding: 10px 12px !important;
    line-height: 1.5 !important;
    word-break: break-word !important;
  }

</style>
</head>
<body>
<div id="canvas-container">
  <canvas id="simCanvas"></canvas>
  <div id="glass-hud">
    <div class="hud-row"><span>Tahap Siklus:</span><span id="hudStage" class="hud-val">1. Ekspansi Isotermal</span></div>
    <div class="hud-row"><span>Efisiensi Termal (η):</span><span id="hudEff" class="hud-val">50.00%</span></div>
    <div class="hud-row"><span>Kerja Bersih (W_net):</span><span id="hudWork" class="hud-val">1728.5 J</span></div>
    <div class="hud-row"><span>Kalor Masuk (Q_H):</span><span id="hudQh" class="hud-val">3457.0 J</span></div>
  </div>
</div>
<div id="control-dock">
  <div class="sliders-grid">
    <div class="control-group">
      <div class="control-label"><span>Reservoir Panas (T_H)</span><span class="val-badge" id="valTh">600 K</span></div>
      <input type="range" id="rangeTh" min="400" max="1000" step="10" value="600">
    </div>
    <div class="control-group">
      <div class="control-label"><span>Reservoir Dingin (T_C)</span><span class="val-badge" id="valTc">300 K</span></div>
      <input type="range" id="rangeTc" min="200" max="500" step="10" value="300">
    </div>
    <div class="control-group">
      <div class="control-label"><span>Volume Awal (V₁)</span><span class="val-badge" id="valV1">1.0 m³</span></div>
      <input type="range" id="rangeV1" min="0.8" max="1.5" step="0.1" value="1.0">
    </div>
    <div class="control-group">
      <div class="control-label"><span>Kecepatan Siklus</span><span class="val-badge" id="valSpeed">1.0x</span></div>
      <input type="range" id="rangeSpeed" min="0.5" max="2.5" step="0.1" value="1.0">
    </div>
  </div>
  <div class="actions-row">
    <div class="btn-group">
      <button id="btnPlay" class="active">Jeda</button>
      <button id="btnStep">Langkah Berikut</button>
    </div>
    <div class="formula-box" id="formulaPill">
      η = 1 - T_C / T_H = 1 - 300 / 600 = 50.00%
    </div>
  </div>
</div>

<script>
(function() {
  const container = document.getElementById('canvas-container');
  const canvas = document.getElementById('simCanvas');
  const ctx = canvas.getContext('2d');

  let width = 600, height = 400;
  function resize() {
    const w = container.clientWidth || 600;
    const h = container.clientHeight || 400;
    const dpr = window.devicePixelRatio || 1;
    canvas.width = Math.round(w * dpr);
    canvas.height = Math.round(h * dpr);
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    width = w;
    height = h;
  }
  window.addEventListener('resize', resize);
  if (window.ResizeObserver) {
    new ResizeObserver(resize).observe(container);
  }
  resize();

  // Params
  let Th = 600;
  let Tc = 300;
  let V1 = 1.0;
  let V2 = 2.0; // Isothermal expansion ratio
  const gamma = 1.4;
  const R = 8.314;
  let speedMult = 1.0;

  let isPlaying = true;
  let cycleProgress = 0.0; // 0.0 to 4.0
  let lastTime = performance.now();

  const rangeTh = document.getElementById('rangeTh');
  const rangeTc = document.getElementById('rangeTc');
  const rangeV1 = document.getElementById('rangeV1');
  const rangeSpeed = document.getElementById('rangeSpeed');
  const valTh = document.getElementById('valTh');
  const valTc = document.getElementById('valTc');
  const valV1 = document.getElementById('valV1');
  const valSpeed = document.getElementById('valSpeed');
  const hudStage = document.getElementById('hudStage');
  const hudEff = document.getElementById('hudEff');
  const hudWork = document.getElementById('hudWork');
  const hudQh = document.getElementById('hudQh');
  const formulaPill = document.getElementById('formulaPill');
  const btnPlay = document.getElementById('btnPlay');
  const btnStep = document.getElementById('btnStep');

  function computeCycle() {
    const P1 = (R * Th) / V1;
    const P2 = (R * Th) / V2;
    // Adiabatic 2->3: Tc * V3^(gamma-1) = Th * V2^(gamma-1)
    const V3 = V2 * Math.pow(Th / Tc, 1 / (gamma - 1));
    const P3 = (R * Tc) / V3;
    // Adiabatic 4->1: Tc * V4^(gamma-1) = Th * V1^(gamma-1)
    const V4 = V1 * Math.pow(Th / Tc, 1 / (gamma - 1));
    const P4 = (R * Tc) / V4;

    const eta = 1 - Tc / Th;
    const Qh = R * Th * Math.log(V2 / V1);
    const Qc = R * Tc * Math.log(V3 / V4);
    const Wnet = Qh - Qc;

    return { P1, P2, P3, P4, V1, V2, V3, V4, eta, Qh, Qc, Wnet };
  }

  function updateHUD() {
    const c = computeCycle();
    hudEff.textContent = (c.eta * 100).toFixed(2) + '%';
    hudWork.textContent = c.Wnet.toFixed(1) + ' J';
    hudQh.textContent = c.Qh.toFixed(1) + ' J';
    formulaPill.textContent = 'η = 1 - (' + Tc + ' / ' + Th + ') = ' + (c.eta * 100).toFixed(2) + '% | W_net = ' + c.Wnet.toFixed(1) + ' J';
  }
  updateHUD();

  rangeTh.oninput = (e) => {
    Th = parseFloat(e.target.value);
    if (Tc >= Th - 30) {
      Tc = Th - 30;
      rangeTc.value = Tc;
      valTc.textContent = Tc + ' K';
    }
    valTh.textContent = Th + ' K';
    updateHUD();
  };

  rangeTc.oninput = (e) => {
    Tc = parseFloat(e.target.value);
    if (Tc >= Th - 30) {
      Th = Tc + 30;
      rangeTh.value = Th;
      valTh.textContent = Th + ' K';
    }
    valTc.textContent = Tc + ' K';
    updateHUD();
  };

  rangeV1.oninput = (e) => {
    V1 = parseFloat(e.target.value);
    V2 = V1 * 2.0;
    valV1.textContent = V1.toFixed(1) + ' m³';
    updateHUD();
  };

  rangeSpeed.oninput = (e) => {
    speedMult = parseFloat(e.target.value);
    valSpeed.textContent = speedMult.toFixed(1) + 'x';
  };

  btnPlay.onclick = () => {
    isPlaying = !isPlaying;
    btnPlay.textContent = isPlaying ? 'Jeda' : 'Lanjutkan';
    btnPlay.classList.toggle('active', isPlaying);
  };

  btnStep.onclick = () => {
    cycleProgress = (Math.floor(cycleProgress) + 1) % 4;
  };

  function getCurrentState(c, progress) {
    const stage = Math.floor(progress) % 4;
    const subT = progress - Math.floor(progress);
    let curV = V1, curP = c.P1, stageName = '1. Ekspansi Isotermal (Reservoir Panas)';
    let temp = Th;

    if (stage === 0) { // 1 -> 2 isothermal Th
      curV = c.V1 + (c.V2 - c.V1) * subT;
      curP = (R * Th) / curV;
      stageName = '1. Ekspansi Isotermal (Menyerap Kalor Q_H)';
      temp = Th;
    } else if (stage === 1) { // 2 -> 3 adiabatic
      curV = c.V2 + (c.V3 - c.V2) * subT;
      curP = c.P2 * Math.pow(c.V2 / curV, gamma);
      stageName = '2. Ekspansi Adiabatik (Terisolasi, Suhu Turun)';
      temp = Th - (Th - Tc) * subT;
    } else if (stage === 2) { // 3 -> 4 isothermal Tc
      curV = c.V3 + (c.V4 - c.V3) * subT;
      curP = (R * Tc) / curV;
      stageName = '3. Kompresi Isotermal (Melepas Kalor Q_C)';
      temp = Tc;
    } else { // 4 -> 1 adiabatic
      curV = c.V4 + (c.V1 - c.V4) * subT;
      curP = c.P4 * Math.pow(c.V4 / curV, gamma);
      stageName = '4. Kompresi Adiabatik (Terisolasi, Suhu Naik)';
      temp = Tc + (Th - Tc) * subT;
    }
    return { curV, curP, stage, stageName, temp };
  }

  // Draw loop
  function draw(ts) {
    const dt = Math.min(0.05, (ts - lastTime) / 1000);
    lastTime = ts;

    if (isPlaying) {
      cycleProgress = (cycleProgress + dt * 0.4 * speedMult) % 4.0;
    }

    ctx.clearRect(0, 0, width, height);

    // Subtle background grid dots
    ctx.fillStyle = 'rgba(0, 0, 0, 0.04)';
    for (let x = 20; x < width; x += 24) {
      for (let y = 20; y < height; y += 24) {
        ctx.fillRect(x, y, 2, 2);
      }
    }

    const c = computeCycle();
    const curr = getCurrentState(c, cycleProgress);
    hudStage.textContent = curr.stageName;

    // Split Canvas: Left side is PV Diagram, Right side is Animated Piston
    const pvW = Math.max(260, width * 0.58);
    const pvH = height - 40;
    const padL = 60, padR = 20, padT = 30, padB = 40;
    const graphW = pvW - padL - padR;
    const graphH = pvH - padT - padB;

    const Vmin = c.V1 * 0.8;
    const Vmax = c.V3 * 1.15;
    const Pmin = c.P3 * 0.7;
    const Pmax = c.P1 * 1.15;

    function x2(V) { return padL + ((V - Vmin) / (Vmax - Vmin)) * graphW; }
    function y2(P) { return padT + graphH - ((P - Pmin) / (Pmax - Pmin)) * graphH; }

    // PV Diagram Border
    ctx.fillStyle = '#FFFFFF';
    ctx.fillRect(padL, padT, graphW, graphH);
    ctx.strokeStyle = '#E2E8F0';
    ctx.lineWidth = 1;
    ctx.strokeRect(padL, padT, graphW, graphH);

    // Gridlines
    ctx.strokeStyle = '#F1F5F9';
    for (let i = 1; i <= 4; i++) {
      const v = Vmin + (i / 5) * (Vmax - Vmin);
      ctx.beginPath(); ctx.moveTo(x2(v), padT); ctx.lineTo(x2(v), padT + graphH); ctx.stroke();
      const p = Pmin + (i / 5) * (Pmax - Pmin);
      ctx.beginPath(); ctx.moveTo(padL, y2(p)); ctx.lineTo(padL + graphW, y2(p)); ctx.stroke();
    }

    // Axes labels
    ctx.fillStyle = '#64748B';
    ctx.font = '10px ui-monospace, Menlo, monospace';
    ctx.fillText('Tekanan P (kPa)', padL + 4, padT - 8);
    ctx.fillText('Volume V (m³)', padL + graphW - 50, padT + graphH + 24);

    // Fill Carnot Cycle Area (Work Done)
    ctx.fillStyle = 'rgba(217, 119, 6, 0.12)';
    ctx.beginPath();
    const steps = 30;
    // 1 -> 2
    for (let i = 0; i <= steps; i++) {
      const v = c.V1 + (c.V2 - c.V1) * i / steps;
      const p = (R * Th) / v;
      if (i === 0) ctx.moveTo(x2(v), y2(p)); else ctx.lineTo(x2(v), y2(p));
    }
    // 2 -> 3
    for (let i = 0; i <= steps; i++) {
      const v = c.V2 + (c.V3 - c.V2) * i / steps;
      const p = c.P2 * Math.pow(c.V2 / v, gamma);
      ctx.lineTo(x2(v), y2(p));
    }
    // 3 -> 4
    for (let i = 0; i <= steps; i++) {
      const v = c.V3 + (c.V4 - c.V3) * i / steps;
      const p = (R * Tc) / v;
      ctx.lineTo(x2(v), y2(p));
    }
    // 4 -> 1
    for (let i = 0; i <= steps; i++) {
      const v = c.V4 + (c.V1 - c.V4) * i / steps;
      const p = c.P4 * Math.pow(c.V4 / v, gamma);
      ctx.lineTo(x2(v), y2(p));
    }
    ctx.closePath();
    ctx.fill();

    // Draw Cycle Curves
    function drawPath(startV, endV, getP, color, isDashed) {
      ctx.strokeStyle = color;
      ctx.lineWidth = 2.5;
      if (isDashed) ctx.setLineDash([4, 4]); else ctx.setLineDash([]);
      ctx.beginPath();
      for (let i = 0; i <= steps; i++) {
        const v = startV + (endV - startV) * i / steps;
        const p = getP(v);
        if (i === 0) ctx.moveTo(x2(v), y2(p)); else ctx.lineTo(x2(v), y2(p));
      }
      ctx.stroke();
      ctx.setLineDash([]);
    }

    drawPath(c.V1, c.V2, (v) => (R * Th) / v, '#DC2626', false); // 1->2 Isotherm Th (Red)
    drawPath(c.V2, c.V3, (v) => c.P2 * Math.pow(c.V2 / v, gamma), '#94A3B8', true); // 2->3 Adiabat (Gray dashed)
    drawPath(c.V3, c.V4, (v) => (R * Tc) / v, '#0284C7', false); // 3->4 Isotherm Tc (Blue)
    drawPath(c.V4, c.V1, (v) => c.P4 * Math.pow(c.V4 / v, gamma), '#94A3B8', true); // 4->1 Adiabat

    // State Corner Dots & Labels
    const pts = [
      { name: '1', v: c.V1, p: c.P1 },
      { name: '2', v: c.V2, p: c.P2 },
      { name: '3', v: c.V3, p: c.P3 },
      { name: '4', v: c.V4, p: c.P4 }
    ];
    pts.forEach((pt) => {
      ctx.fillStyle = '#0F172A';
      ctx.beginPath();
      ctx.arc(x2(pt.v), y2(pt.p), 4, 0, Math.PI * 2);
      ctx.fill();
      ctx.font = 'bold 11px ui-monospace, Menlo, monospace';
      ctx.fillText(pt.name, x2(pt.v) + 6, y2(pt.p) - 4);
    });

    // Active Moving Working Point on PV Diagram
    const curSx = x2(curr.curV);
    const curSy = y2(curr.curP);
    ctx.fillStyle = '#D97706';
    ctx.beginPath();
    ctx.arc(curSx, curSy, 7, 0, Math.PI * 2);
    ctx.fill();
    ctx.strokeStyle = '#FFFFFF';
    ctx.lineWidth = 2;
    ctx.stroke();

    // -------------------------------------------------------------
    // Right Side: Animated Cylinder & Piston
    // -------------------------------------------------------------
    const pistonAreaX = pvW + 20;
    const pistonAreaW = width - pistonAreaX - 20;
    if (pistonAreaW > 120) {
      const cylW = Math.min(180, pistonAreaW);
      const cylH = 220;
      const cylX = pistonAreaX + (pistonAreaW - cylW) / 2;
      const cylY = padT + 20;

      // Base Thermal Reservoir color depending on stage
      let baseColor = '#CBD5E1'; // Insulated
      let baseLabel = 'Insulator Adiabatik';
      if (curr.stage === 0) {
        baseColor = '#FEE2E2'; // Hot
        baseLabel = 'Reservoir Panas (' + Th + ' K)';
      } else if (curr.stage === 2) {
        baseColor = '#E0F2FE'; // Cold
        baseLabel = 'Reservoir Dingin (' + Tc + ' K)';
      }

      // Cylinder Walls
      ctx.fillStyle = '#F8FAFC';
      ctx.fillRect(cylX, cylY, cylW, cylH);
      ctx.strokeStyle = '#64748B';
      ctx.lineWidth = 3;
      ctx.beginPath();
      ctx.moveTo(cylX, cylY);
      ctx.lineTo(cylX, cylY + cylH);
      ctx.lineTo(cylX + cylW, cylY + cylH);
      ctx.lineTo(cylX + cylW, cylY);
      ctx.stroke();

      // Thermal Reservoir Base Block
      ctx.fillStyle = baseColor;
      ctx.fillRect(cylX - 10, cylY + cylH, cylW + 20, 28);
      ctx.strokeStyle = '#94A3B8';
      ctx.lineWidth = 1.5;
      ctx.strokeRect(cylX - 10, cylY + cylH, cylW + 20, 28);
      ctx.fillStyle = '#0F172A';
      ctx.font = 'bold 10px ui-monospace, Menlo, monospace';
      ctx.textAlign = 'center';
      ctx.fillText(baseLabel, cylX + cylW / 2, cylY + cylH + 18);

      // Piston Position linked to Current Volume (curV)
      const vNorm = (curr.curV - c.V1) / (c.V3 - c.V1); // 0 (compressed) to 1 (expanded)
      const pistonMinY = cylY + 40;
      const pistonMaxY = cylY + cylH - 60;
      // High volume = piston higher up (lower Y)
      const pistonY = pistonMaxY - vNorm * (pistonMaxY - pistonMinY);

      // Gas inside cylinder
      const gasAlpha = 0.08 + (curr.temp / 1000) * 0.2;
      ctx.fillStyle = 'rgba(220, 38, 38, ' + gasAlpha + ')';
      ctx.fillRect(cylX + 2, pistonY + 16, cylW - 4, (cylY + cylH) - (pistonY + 16));

      // Piston Head
      ctx.fillStyle = '#475569';
      ctx.fillRect(cylX + 2, pistonY, cylW - 4, 16);
      ctx.strokeStyle = '#0F172A';
      ctx.lineWidth = 1;
      ctx.strokeRect(cylX + 2, pistonY, cylW - 4, 16);

      // Piston Rod
      ctx.fillStyle = '#94A3B8';
      ctx.fillRect(cylX + cylW / 2 - 8, cylY - 20, 16, pistonY - (cylY - 20));
      ctx.strokeStyle = '#475569';
      ctx.strokeRect(cylX + cylW / 2 - 8, cylY - 20, 16, pistonY - (cylY - 20));

      // Gas Temperature & Pressure readout inside cylinder
      ctx.fillStyle = '#0F172A';
      ctx.font = 'bold 11px ui-monospace, Menlo, monospace';
      ctx.fillText('T = ' + Math.round(curr.temp) + ' K', cylX + cylW / 2, (cylY + cylH + pistonY) / 2);
    }

    requestAnimationFrame(draw);
  }
  requestAnimationFrame(draw);
})();
</script>
</body>
</html>
"""

    fun getAcidBaseTitrationHtml(): String = """
﻿<!DOCTYPE html>
<html lang="id">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; -webkit-tap-highlight-color: transparent; }
  html, body { width: 100%; height: 100%; overflow: hidden; display: flex; flex-direction: column; background: #FFFFFF; color: #0F172A; user-select: none; -webkit-user-select: none; }
  #canvas-container { flex: 1 1 0%; min-height: 0; position: relative; width: 100%; background: #FFFFFF; overflow: hidden; }
  canvas { position: absolute; top: 0; left: 0; width: 100%; height: 100%; display: block; touch-action: none; }
  #glass-hud { position: absolute; top: 14px; left: 14px; background: rgba(255, 255, 255, 0.94); backdrop-filter: blur(8px); border: 1px solid #E2E8F0; border-radius: 12px; padding: 10px 14px; box-shadow: 0 4px 14px rgba(0,0,0,0.06); font-size: 12px; line-height: 1.6; max-width: 320px; pointer-events: none; z-index: 5; }
  .hud-row { display: flex; justify-content: space-between; gap: 12px; }
  .hud-val { font-weight: 700; color: #059669; font-family: ui-monospace, Menlo, Consolas, monospace; }
  #control-dock { flex-shrink: 0; background: #F8FAFC; border-top: 1px solid #E2E8F0; padding: 12px 18px; display: flex; flex-direction: column; gap: 10px; z-index: 10; }
  .sliders-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 12px; }
  .control-group { display: flex; flex-direction: column; gap: 4px; }
  .control-label { display: flex; justify-content: space-between; font-size: 11px; font-weight: 600; color: #64748B; }
  .val-badge { font-weight: 700; color: #059669; font-family: ui-monospace, Menlo, monospace; }
  input[type="range"] { width: 100%; height: 26px; accent-color: #059669; cursor: pointer; }
  select { background: #FFFFFF; border: 1px solid #CBD5E1; border-radius: 6px; padding: 5px 8px; font-size: 12px; font-weight: 600; color: #0F172A; }
  .actions-row { display: flex; align-items: center; justify-content: space-between; gap: 10px; border-top: 1px solid #E2E8F0; padding-top: 8px; }
  .btn-group { display: flex; gap: 8px; }
  button { padding: 8px 14px; font-size: 12px; font-weight: 600; border-radius: 8px; cursor: pointer; border: 1px solid #CBD5E1; background: #FFFFFF; color: #0F172A; }
  button.active { background: #0F172A; color: #FFFFFF; border-color: #0F172A; }
  .formula-box { font-family: ui-monospace, Menlo, Consolas, monospace; font-size: 11px; color: #334155; background: #FFFFFF; border: 1px solid #E2E8F0; border-radius: 6px; padding: 6px 12px; }

  /* ── Studio Workbench Layout Upgrade ── */
  html, body {
    position: fixed !important;
    inset: 0 !important;
    width: 100vw !important;
    height: 100vh !important;
    margin: 0 !important;
    padding: 0 !important;
    overflow: hidden !important;
    display: flex !important;
    flex-direction: row !important;
    background: #FFFFFF !important;
    color: #0F172A !important;
    user-select: none !important;
    -webkit-user-select: none !important;
  }
  #canvas-container, #main-container {
    flex: 1 1 0% !important;
    min-width: 0 !important;
    height: 100% !important;
    position: relative !important;
    background: #FFFFFF !important;
    overflow: hidden !important;
  }
  #control-dock, #inspector-sidebar {
    width: 340px !important;
    max-width: 35vw !important;
    flex-shrink: 0 !important;
    height: 100% !important;
    background: #F8FAFC !important;
    border-left: 1px solid #E2E8F0 !important;
    border-top: none !important;
    padding: 20px 18px !important;
    display: flex !important;
    flex-direction: column !important;
    gap: 14px !important;
    overflow-y: auto !important;
    z-index: 10 !important;
    box-shadow: -4px 0 16px rgba(0,0,0,0.02) !important;
  }
  .sliders-grid {
    display: flex !important;
    flex-direction: column !important;
    gap: 12px !important;
  }
  .control-group {
    display: flex !important;
    flex-direction: column !important;
    gap: 6px !important;
    background: #FFFFFF !important;
    border: 1px solid #E2E8F0 !important;
    border-radius: 10px !important;
    padding: 10px 12px !important;
    box-shadow: 0 1px 3px rgba(0,0,0,0.02) !important;
  }
  .actions-row {
    display: flex !important;
    flex-direction: column !important;
    gap: 10px !important;
    border-top: 1px solid #E2E8F0 !important;
    padding-top: 14px !important;
    margin-top: auto !important;
  }
  .btn-group {
    display: flex !important;
    gap: 8px !important;
    width: 100% !important;
  }
  .btn-group button {
    flex: 1 !important;
  }
  .formula-box, .theory-card {
    font-family: ui-monospace, Menlo, Consolas, monospace !important;
    font-size: 11px !important;
    color: #0369A1 !important;
    background: #EFF6FF !important;
    border: 1px solid #BFDBFE !important;
    border-radius: 8px !important;
    padding: 10px 12px !important;
    line-height: 1.5 !important;
    word-break: break-word !important;
  }

</style>
</head>
<body>
<div id="canvas-container">
  <canvas id="simCanvas"></canvas>
  <div id="glass-hud">
    <div class="hud-row"><span>Status Titrasi:</span><span id="hudStatus" class="hud-val">Sebelum Titik Ekuivalen</span></div>
    <div class="hud-row"><span>Volume NaOH (V_b):</span><span id="hudVol" class="hud-val">0.00 mL</span></div>
    <div class="hud-row"><span>Derajat Keasaman (pH):</span><span id="hudPh" class="hud-val">1.00</span></div>
    <div class="hud-row"><span>Titik Ekuivalen:</span><span id="hudEq" class="hud-val">25.0 mL</span></div>
  </div>
</div>
<div id="control-dock">
  <div class="sliders-grid">
    <div class="control-group">
      <div class="control-label"><span>Sampel Asam (25 mL, 0.1M)</span></div>
      <select id="selectAcid">
        <option value="strong" selected>HCl (Asam Kuat)</option>
        <option value="weak">CH₃COOH (Asam Lemah, Ka = 1.8e-5)</option>
      </select>
    </div>
    <div class="control-group">
      <div class="control-label"><span>Indikator Asam-Basa</span></div>
      <select id="selectIndicator">
        <option value="pp" selected>Fenolftalein (PP, 8.2 - 10.0)</option>
        <option value="btb">Bromotimol Biru (BTB, 6.0 - 7.6)</option>
        <option value="mo">Metil Jingga (MO, 3.1 - 4.4)</option>
      </select>
    </div>
    <div class="control-group">
      <div class="control-label"><span>Volume NaOH Ditambahkan</span><span class="val-badge" id="valVol">0.0 mL</span></div>
      <input type="range" id="rangeVol" min="0" max="50" step="0.2" value="0">
    </div>
    <div class="control-group">
      <div class="control-label"><span>Laju Aliran Buret</span><span class="val-badge" id="valFlow">Diam</span></div>
      <div class="btn-group">
        <button id="btnDrip" style="flex:1;">Tetes (+0.5mL)</button>
        <button id="btnAutoDrip" style="flex:1;">Auto-Tetes</button>
      </div>
    </div>
  </div>
  <div class="actions-row">
    <div class="btn-group">
      <button id="btnReset">Reset Titrasi</button>
    </div>
    <div class="formula-box" id="formulaPill">
      Titik Ekuivalen: V_a · M_a = V_b · M_b → V_eq = 25.0 mL
    </div>
  </div>
</div>

<script>
(function() {
  const container = document.getElementById('canvas-container');
  const canvas = document.getElementById('simCanvas');
  const ctx = canvas.getContext('2d');

  let width = 600, height = 400;
  function resize() {
    const w = container.clientWidth || 600;
    const h = container.clientHeight || 400;
    const dpr = window.devicePixelRatio || 1;
    canvas.width = Math.round(w * dpr);
    canvas.height = Math.round(h * dpr);
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    width = w;
    height = h;
  }
  window.addEventListener('resize', resize);
  if (window.ResizeObserver) {
    new ResizeObserver(resize).observe(container);
  }
  resize();

  // Params
  let acidType = 'strong';
  let indicator = 'pp';
  let volNaOH = 0.0; // 0 to 50 mL
  const Va = 25.0; // mL
  const Ca = 0.10; // M
  const Cb = 0.10; // M
  const Ka = 1.8e-5;

  let isAutoDripping = false;
  let droplets = []; // [{x, y, vy}]

  // DOM
  const selectAcid = document.getElementById('selectAcid');
  const selectIndicator = document.getElementById('selectIndicator');
  const rangeVol = document.getElementById('rangeVol');
  const valVol = document.getElementById('valVol');
  const valFlow = document.getElementById('valFlow');
  const hudStatus = document.getElementById('hudStatus');
  const hudVol = document.getElementById('hudVol');
  const hudPh = document.getElementById('hudPh');
  const hudEq = document.getElementById('hudEq');
  const formulaPill = document.getElementById('formulaPill');
  const btnDrip = document.getElementById('btnDrip');
  const btnAutoDrip = document.getElementById('btnAutoDrip');
  const btnReset = document.getElementById('btnReset');

  // Exact charge balance pH computation
  function computePH(vBase) {
    const moleA = (Va / 1000) * Ca;
    const moleB = (vBase / 1000) * Cb;
    const Vtot = (Va + vBase) / 1000; // L

    if (acidType === 'strong') {
      if (moleA > moleB + 1e-11) {
        const Hplus = (moleA - moleB) / Vtot;
        return -Math.log10(Math.max(Hplus, 1e-14));
      }
      if (moleB > moleA + 1e-11) {
        const OHm = (moleB - moleA) / Vtot;
        return 14 + Math.log10(Math.max(OHm, 1e-14));
      }
      return 7.0; // exact neutral
    }

    // Weak acid exact bisection
    const CT = moleA / Vtot;
    const Na = moleB / Vtot;
    const f = (h) => Na + h - (CT * Ka) / (Ka + h) - 1e-14 / h;
    let lo = -1, hi = 15;
    for (let i = 0; i < 55; i++) {
      const mid = (lo + hi) / 2;
      if (f(Math.pow(10, -mid)) > 0) lo = mid; else hi = mid;
    }
    return (lo + hi) / 2;
  }

  function getIndicatorColor(ph) {
    if (indicator === 'pp') {
      // Phenolphthalein: colorless below 8.2, intense pink/fuchsia above 10.0
      if (ph < 8.2) return 'rgba(241, 245, 249, 0.4)';
      const t = Math.min(1, (ph - 8.2) / 1.8);
      return 'rgba(236, 72, 153, ' + (0.15 + t * 0.75) + ')';
    } else if (indicator === 'btb') {
      // Bromothymol blue: yellow below 6.0, green ~7.0, blue above 7.6
      if (ph < 6.0) return 'rgba(234, 179, 8, 0.7)';
      if (ph > 7.6) return 'rgba(37, 99, 235, 0.75)';
      const t = (ph - 6.0) / 1.6;
      const r = Math.round(234 * (1 - t) + 37 * t);
      const g = Math.round(179 * (1 - t) + 99 * t);
      const b = Math.round(8 * (1 - t) + 235 * t);
      return 'rgba(' + r + ', ' + g + ', ' + b + ', 0.7)';
    } else {
      // Methyl orange: red below 3.1, orange ~3.8, yellow above 4.4
      if (ph < 3.1) return 'rgba(239, 68, 68, 0.8)';
      if (ph > 4.4) return 'rgba(245, 158, 11, 0.8)';
      return 'rgba(249, 115, 22, 0.8)';
    }
  }

  function updateHUD() {
    const ph = computePH(volNaOH);
    hudVol.textContent = volNaOH.toFixed(1) + ' mL';
    hudPh.textContent = ph.toFixed(2);
    valVol.textContent = volNaOH.toFixed(1) + ' mL';

    if (Math.abs(volNaOH - 25.0) < 0.3) {
      hudStatus.textContent = 'Mencapai Titik Ekuivalen!';
      hudStatus.style.color = '#7C3AED';
    } else if (volNaOH < 25.0) {
      hudStatus.textContent = 'Sebelum Titik Ekuivalen (Kelebihan Asam)';
      hudStatus.style.color = '#0284C7';
    } else {
      hudStatus.textContent = 'Melewati Titik Ekuivalen (Kelebihan Basa)';
      hudStatus.style.color = '#D97706';
    }

    const eqPH = acidType === 'strong' ? '7.00' : '8.72';
    hudEq.textContent = '25.0 mL (pH = ' + eqPH + ')';
    formulaPill.textContent = (acidType === 'strong' ? 'HCl' : 'CH₃COOH') + ' + NaOH → pH = ' + ph.toFixed(2) + ' | V_eq = 25.0 mL';
  }
  updateHUD();

  selectAcid.onchange = (e) => { acidType = e.target.value; updateHUD(); };
  selectIndicator.onchange = (e) => { indicator = e.target.value; updateHUD(); };
  rangeVol.oninput = (e) => {
    volNaOH = parseFloat(e.target.value);
    updateHUD();
  };

  btnDrip.onclick = () => {
    volNaOH = Math.min(50, volNaOH + 0.5);
    rangeVol.value = volNaOH;
    updateHUD();
  };

  btnAutoDrip.onclick = () => {
    isAutoDripping = !isAutoDripping;
    btnAutoDrip.textContent = isAutoDripping ? 'Stop Auto' : 'Auto-Tetes';
    btnAutoDrip.classList.toggle('active', isAutoDripping);
    valFlow.textContent = isAutoDripping ? 'Menetes...' : 'Diam';
  };

  btnReset.onclick = () => {
    volNaOH = 0;
    isAutoDripping = false;
    btnAutoDrip.textContent = 'Auto-Tetes';
    btnAutoDrip.classList.remove('active');
    valFlow.textContent = 'Diam';
    rangeVol.value = 0;
    droplets = [];
    updateHUD();
  };

  // Draw loop
  let lastTime = performance.now();
  function draw(ts) {
    const dt = Math.min(0.05, (ts - lastTime) / 1000);
    lastTime = ts;

    if (isAutoDripping && volNaOH < 50) {
      volNaOH = Math.min(50, volNaOH + dt * 2.5);
      rangeVol.value = volNaOH;
      updateHUD();
    } else if (isAutoDripping && volNaOH >= 50) {
      isAutoDripping = false;
      btnAutoDrip.textContent = 'Auto-Tetes';
      btnAutoDrip.classList.remove('active');
      valFlow.textContent = 'Penuh (50 mL)';
    }

    ctx.clearRect(0, 0, width, height);

    // Subtle dot grid
    ctx.fillStyle = 'rgba(0, 0, 0, 0.04)';
    for (let x = 20; x < width; x += 24) {
      for (let y = 20; y < height; y += 24) {
        ctx.fillRect(x, y, 2, 2);
      }
    }

    const curPH = computePH(volNaOH);

    // Left Side: Burette & Erlenmeyer Flask apparatus
    const labW = Math.max(200, width * 0.38);
    const flaskCenterX = labW / 2 + 10;
    const flaskBottomY = height - 45;

    // Burette (Buret kaca)
    const buretteW = 20;
    const buretteTopY = 30;
    const buretteBottomY = flaskBottomY - 140;
    const buretteX = flaskCenterX - buretteW / 2;

    ctx.fillStyle = 'rgba(255, 255, 255, 0.8)';
    ctx.fillRect(buretteX, buretteTopY, buretteW, buretteBottomY - buretteTopY);
    ctx.strokeStyle = '#94A3B8';
    ctx.lineWidth = 1.5;
    ctx.strokeRect(buretteX, buretteTopY, buretteW, buretteBottomY - buretteTopY);

    // Liquid inside burette (NaOH level decreases as volNaOH increases)
    const liquidTopY = buretteTopY + (volNaOH / 50) * (buretteBottomY - buretteTopY - 20);
    ctx.fillStyle = 'rgba(14, 165, 233, 0.25)';
    ctx.fillRect(buretteX + 1, liquidTopY, buretteW - 2, (buretteBottomY - 10) - liquidTopY);

    // Burette graduations
    ctx.strokeStyle = '#CBD5E1';
    ctx.lineWidth = 1;
    for (let ml = 0; ml <= 50; ml += 10) {
      const gy = buretteTopY + (ml / 50) * (buretteBottomY - buretteTopY - 20);
      ctx.beginPath();
      ctx.moveTo(buretteX + buretteW, gy);
      ctx.lineTo(buretteX + buretteW + 6, gy);
      ctx.stroke();
    }

    // Burette tip & valve
    ctx.fillStyle = '#64748B';
    ctx.fillRect(flaskCenterX - 3, buretteBottomY - 10, 6, 16);
    ctx.fillRect(flaskCenterX - 8, buretteBottomY - 4, 16, 4);

    // Falling droplet
    if (isAutoDripping && Math.random() < 0.25) {
      droplets.push({ x: flaskCenterX, y: buretteBottomY + 8, vy: 120 });
    }
    ctx.fillStyle = '#0284C7';
    for (let i = droplets.length - 1; i >= 0; i--) {
      const d = droplets[i];
      d.y += d.vy * dt;
      ctx.beginPath();
      ctx.arc(d.x, d.y, 2.5, 0, Math.PI * 2);
      ctx.fill();
      if (d.y > flaskBottomY - 30) droplets.splice(i, 1);
    }

    // Erlenmeyer Flask
    const neckTopY = flaskBottomY - 100;
    const neckW = 28;
    const baseW = 100;

    // Solution inside flask
    const solColor = getIndicatorColor(curPH);
    ctx.fillStyle = solColor;
    ctx.beginPath();
    ctx.moveTo(flaskCenterX - neckW / 2 + 2, neckTopY + 35);
    ctx.lineTo(flaskCenterX - baseW / 2 + 4, flaskBottomY - 4);
    ctx.lineTo(flaskCenterX + baseW / 2 - 4, flaskBottomY - 4);
    ctx.lineTo(flaskCenterX + neckW / 2 - 2, neckTopY + 35);
    ctx.closePath();
    ctx.fill();

    // Flask Glass Outline
    ctx.strokeStyle = '#64748B';
    ctx.lineWidth = 2.5;
    ctx.beginPath();
    ctx.moveTo(flaskCenterX - neckW / 2, neckTopY);
    ctx.lineTo(flaskCenterX - neckW / 2, neckTopY + 30);
    ctx.lineTo(flaskCenterX - baseW / 2, flaskBottomY);
    ctx.lineTo(flaskCenterX + baseW / 2, flaskBottomY);
    ctx.lineTo(flaskCenterX + neckW / 2, neckTopY + 30);
    ctx.lineTo(flaskCenterX + neckW / 2, neckTopY);
    ctx.stroke();

    // Lab apparatus base stand
    ctx.fillStyle = '#E2E8F0';
    ctx.fillRect(flaskCenterX - baseW / 2 - 15, flaskBottomY, baseW + 30, 8);

    // -------------------------------------------------------------
    // Right Side: Titration S-Curve Graph (pH vs Volume NaOH)
    // -------------------------------------------------------------
    const graphLeft = labW + 30;
    const graphRight = width - 25;
    const graphTop = 35;
    const graphBottom = height - 45;
    const gW = graphRight - graphLeft;
    const gH = graphBottom - graphTop;

    function v2x(v) { return graphLeft + (v / 50) * gW; }
    function p2y(p) { return graphBottom - (p / 14) * gH; }

    // Graph background
    ctx.fillStyle = '#FFFFFF';
    ctx.fillRect(graphLeft, graphTop, gW, gH);
    ctx.strokeStyle = '#E2E8F0';
    ctx.lineWidth = 1;
    ctx.strokeRect(graphLeft, graphTop, gW, gH);

    // Equivalence Vertical Guideline (V = 25 mL)
    ctx.strokeStyle = '#CBD5E1';
    ctx.lineWidth = 1.5;
    ctx.setLineDash([4, 4]);
    ctx.beginPath();
    ctx.moveTo(v2x(25), graphTop);
    ctx.lineTo(v2x(25), graphBottom);
    ctx.stroke();
    ctx.setLineDash([]);

    // pH 7 Neutral Guideline
    ctx.strokeStyle = '#E2E8F0';
    ctx.beginPath();
    ctx.moveTo(graphLeft, p2y(7));
    ctx.lineTo(graphRight, p2y(7));
    ctx.stroke();

    // Graph Axes Labels
    ctx.fillStyle = '#64748B';
    ctx.font = '10px ui-monospace, Menlo, monospace';
    ctx.fillText('pH', graphLeft + 6, graphTop - 8);
    ctx.fillText('Volume NaOH (mL)', graphRight - 90, graphBottom + 22);

    for (let p = 0; p <= 14; p += 2) {
      ctx.fillText(p, graphLeft - 18, p2y(p) + 4);
    }
    for (let v = 0; v <= 50; v += 10) {
      ctx.fillText(v, v2x(v) - 6, graphBottom + 16);
    }

    // Draw Complete Theoretical Titration S-Curve
    ctx.strokeStyle = '#059669';
    ctx.lineWidth = 2.5;
    ctx.beginPath();
    for (let i = 0; i <= 200; i++) {
      const v = (i / 200) * 50;
      const ph = computePH(v);
      const sx = v2x(v);
      const sy = p2y(ph);
      if (i === 0) ctx.moveTo(sx, sy); else ctx.lineTo(sx, sy);
    }
    ctx.stroke();

    // Active Titration Progress Point
    const curSx = v2x(volNaOH);
    const curSy = p2y(curPH);

    // Crosshairs
    ctx.strokeStyle = '#94A3B8';
    ctx.lineWidth = 1;
    ctx.setLineDash([3, 3]);
    ctx.beginPath();
    ctx.moveTo(curSx, graphBottom);
    ctx.lineTo(curSx, curSy);
    ctx.lineTo(graphLeft, curSy);
    ctx.stroke();
    ctx.setLineDash([]);

    // Dot
    ctx.fillStyle = '#059669';
    ctx.beginPath();
    ctx.arc(curSx, curSy, 6, 0, Math.PI * 2);
    ctx.fill();
    ctx.strokeStyle = '#FFFFFF';
    ctx.lineWidth = 2;
    ctx.stroke();

    requestAnimationFrame(draw);
  }
  requestAnimationFrame(draw);
})();
</script>
</body>
</html>
"""
}
