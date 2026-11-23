/**
 * GeoNix 3D Studio - Multi-Object Construction Engine
 * Supports simultaneous multi-object plotting:
 * - 2D Curves: y = f(x)
 * - Solids of Revolution: Rotated around X/Y axis with dynamic angle (0 - 360°)
 * - 3D Multivariable Surfaces: z = f(x, y)
 * - 3D Vectors (R³) with dynamic shaft/head & arithmetic
 * - 3D Points with coordinates & Sprite labels
 * - 3D Planes: ax + by + cz = d
 * - 3D Polyhedra: Cube, Cuboid, Prism, Pyramid, Sphere, Cylinder, Cone
 * - Dynamic Variable Sliders: Real-time reactive updates
 */

(function () {
    'use strict';

    window.Math3D = {};

    let scene, camera, renderer, controls;
    let gridHelper, axesGroup;
    let objectsRootGroup;

    // Construction State
    const construction = {
        elements: new Map(), // id -> { id, type, config, threeObj, metrics }
        variables: new Map([ // variableName -> value
            ['a', 2],
            ['b', 1],
            ['k', 3],
            ['t', 1]
        ]),
        view: {
            showGrid: true,
            showAxes: true,
            theme: 'light', // 'dark' | 'light'
            mode: '3d' // permanent 3D Space
        }
    };

    const COLORS = {
        axisX: 0xEF5350,
        axisY: 0x66BB6A,
        axisZ: 0x42A5F5,
        defaultPalette: [
            0x2563EB, // Blue
            0x00E5FF, // Cyan
            0xFFD600, // Amber
            0x00E676, // Green
            0xE040FB, // Purple
            0xFF5722, // Orange
            0xE91E63  // Pink
        ]
    };

    /**
     * Solves and normalizes equations like y^2 = x, x = y^2, y = 2x, etc. into expressions of x
     */
    function normalizeEquation(exprStr) {
        if (!exprStr || typeof exprStr !== 'string') return '';
        let clean = exprStr.trim();

        clean = clean
            .replace(/\\left\(|\\right\)/g, '(')
            .replace(/left\(|right\)/g, '(')
            .replace(/\\sqrt\{([^}]+)\}/g, 'sqrt($1)')
            .replace(/\\frac\{([^}]+)\}\{([^}]+)\}/g, '(($1)/($2))')
            .replace(/\\cdot/g, '*')
            .replace(/·/g, '*')
            .replace(/\\times/g, '*')
            .replace(/\\/g, '');

        if (!clean.includes('=')) {
            return clean;
        }

        const parts = clean.split('=');
        let lhs = parts[0].trim().replace(/\s+/g, '');
        let rhs = parts.slice(1).join('=').trim().replace(/\s+/g, '');

        // Detect LHS like y^2, y^{2}, y², y*y, a*y^2
        const lhsY2Match = lhs.match(/^([+-]?\d*\.?\d*\*?)?y(\^2|\^\{2\}|²|\*y)$/i);
        if (lhsY2Match) {
            let coeffStr = lhsY2Match[1] ? lhsY2Match[1].replace('*', '') : '';
            let coeff = (coeffStr === '' || coeffStr === '+') ? 1 : (coeffStr === '-' ? -1 : parseFloat(coeffStr));
            if (!isNaN(coeff) && coeff !== 0) {
                if (coeff === 1) {
                    return `sqrt(${rhs})`;
                } else {
                    return `sqrt((${rhs}) / (${coeff}))`;
                }
            }
            return `sqrt(${rhs})`;
        }

        // Detect RHS like y^2, y^{2}, y², y*y, a*y^2 when LHS is x (e.g. x = y^2)
        const rhsY2Match = rhs.match(/^([+-]?\d*\.?\d*\*?)?y(\^2|\^\{2\}|²|\*y)$/i);
        if (rhsY2Match && (lhs === 'x' || lhs === 'x(y)')) {
            let coeffStr = rhsY2Match[1] ? rhsY2Match[1].replace('*', '') : '';
            let coeff = (coeffStr === '' || coeffStr === '+') ? 1 : (coeffStr === '-' ? -1 : parseFloat(coeffStr));
            if (!isNaN(coeff) && coeff !== 0) {
                if (coeff === 1) {
                    return `sqrt(x)`;
                } else {
                    return `sqrt(x / (${coeff}))`;
                }
            }
            return `sqrt(x)`;
        }

        // Standard explicit y = f(x) or f(x) = ...
        if (/^(y|f\(x\)|g\(x\)|h\(x\)|z)$/i.test(lhs)) {
            return rhs;
        }

        // If lhs is x = f(y) and rhs only uses y, e.g. x = y -> y = x
        if (lhs === 'x' && !rhs.includes('x')) {
            return rhs.replace(/y/g, 'x');
        }

        return rhs;
    }

    /**
     * Mathematical Formula Compiler
     * Converts math expressions like "x^2 + 2x - sin(y)" into fast JS functions.
     */
    function compileMath(exprStr, varNames) {
        if (!exprStr || typeof exprStr !== 'string') return null;
        let clean = normalizeEquation(exprStr);

        if (clean.includes(':')) {
            clean = clean.split(':').pop().trim();
        }

        // 2. Strip LaTeX brackets, symbols & leftovers
        clean = clean
            .replace(/\\left\(|\\right\)/g, '')
            .replace(/left\(|right\)/g, '')
            .replace(/\\sqrt\{([^}]+)\}/g, 'sqrt($1)')
            .replace(/\\frac\{([^}]+)\}\{([^}]+)\}/g, '(($1)/($2))')
            .replace(/\\cdot/g, '*')
            .replace(/·/g, '*')
            .replace(/\\times/g, '*')
            .replace(/\\/g, '');

        clean = clean
            .replace(/\s+/g, '')
            .replace(/(\d)([a-zA-Z(])/g, '$1*$2') // 2x -> 2*x, 2( -> 2*(
            .replace(/([a-zA-Z\)])(\d)/g, '$1*$2')
            .replace(/\^/g, '**')
            .replace(/\bsin\b/gi, 'Math.sin')
            .replace(/\bcos\b/gi, 'Math.cos')
            .replace(/\btan\b/gi, 'Math.tan')
            .replace(/\bsqrt\b/gi, 'Math.sqrt')
            .replace(/\babs\b/gi, 'Math.abs')
            .replace(/\bexp\b/gi, 'Math.exp')
            .replace(/\bln\b/gi, 'Math.log')
            .replace(/\bpi\b/gi, 'Math.PI')
            .replace(/\be\b/gi, 'Math.E');

        // Substitute known dynamic variable values (e.g. 'a', 'k')
        construction.variables.forEach((val, varName) => {
            if (!varNames.includes(varName)) {
                const regex = new RegExp(`\\b${varName}\\b`, 'g');
                clean = clean.replace(regex, `(${val})`);
            }
        });

        try {
            return new Function(...varNames, `try { return (${clean}); } catch(e) { return NaN; }`);
        } catch (e) {
            console.warn('Failed to compile expression:', exprStr, 'Cleaned:', clean, e);
            return null;
        }
    }

    /**
     * Initialize 3D Engine & Scene
     */
    function init() {
        const container = document.getElementById('viewport');
        const width = container.clientWidth || window.innerWidth;
        const height = container.clientHeight || window.innerHeight;

        scene = new THREE.Scene();
        scene.background = new THREE.Color(construction.view.theme === 'dark' ? 0x0F131C : 0xFFFFFF);

        // Z-up coordinate system (standard GeoNix)
        camera = new THREE.PerspectiveCamera(45, width / height, 0.1, 1000);
        camera.up.set(0, 1, 0);
        camera.position.set(14, 12, 18);
        camera.lookAt(0, 0, 0);

        renderer = new THREE.WebGLRenderer({
            antialias: true,
            preserveDrawingBuffer: true,
            powerPreference: 'high-performance'
        });
        renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2.5));
        renderer.setSize(width, height);
        container.appendChild(renderer.domElement);

        controls = new THREE.OrbitControls(camera, renderer.domElement);
        controls.enableDamping = true;
        controls.dampingFactor = 0.09;
        controls.rotateSpeed = 0.95;
        controls.zoomSpeed = 1.0;
        controls.panSpeed = 1.0;

        // 3D Space View & Controls (1-finger orbit, 2-finger tilt/pitch/zoom/pan)
        camera.position.set(14, 12, 18);
        camera.up.set(0, 1, 0);
        camera.lookAt(0, 0, 0);
        controls.target.set(0, 0, 0);
        controls.enableRotate = true;
        controls.minPolarAngle = 0.04;
        controls.maxPolarAngle = Math.PI - 0.04;
        controls.minDistance = 3.5;
        controls.maxDistance = 45.0;
        controls.screenSpacePanning = false;
        controls.touches.ONE = THREE.TOUCH.ROTATE;
        controls.touches.TWO = THREE.TOUCH.DOLLY_ROTATE; // 2-finger: Pinch = Zoom, Vertical Drag = Tilt / Pitch (view from above/below)
        controls.mouseButtons.LEFT = THREE.MOUSE.ROTATE;

        // Lighting (Ambient + Two Directional Lights)
        const ambientLight = new THREE.AmbientLight(0xFFFFFF, 0.7);
        scene.add(ambientLight);

        const dirLight1 = new THREE.DirectionalLight(0xFFFFFF, 0.7);
        dirLight1.position.set(15, 20, 15);
        scene.add(dirLight1);

        const dirLight2 = new THREE.DirectionalLight(0xB0BEC5, 0.4);
        dirLight2.position.set(-15, -10, -15);
        scene.add(dirLight2);

        gridHelper = new THREE.Group();
        axesGroup = new THREE.Group();
        objectsRootGroup = new THREE.Group();

        scene.add(gridHelper);
        scene.add(axesGroup);
        scene.add(objectsRootGroup);

        buildGridAndAxes();

        window.addEventListener('resize', onWindowResize, false);
        animate();

        // Notify Android Kotlin bridge
        notifyBridge('onEngineReady', { status: 'ready' });
    }

    function formatNumberLabel(val, step) {
        if (Math.abs(val) < 1e-9) return '0';
        if (step >= 1) return Math.round(val).toString();
        const decimals = Math.max(1, Math.min(3, Math.ceil(-Math.log10(step))));
        return Number(val.toFixed(decimals)).toString();
    }

    function makeTextSprite(message, opts = {}) {
        const textColor = opts.textColor || '#FFFFFF';
        const fontSize = opts.fontSize || 48;
        const canvas = document.createElement('canvas');
        canvas.width = 256;
        canvas.height = 128;
        const ctx = canvas.getContext('2d');
        ctx.font = `bold ${fontSize}px Inter, system-ui, -apple-system, sans-serif`;
        ctx.fillStyle = typeof textColor === 'number' ? `#${textColor.toString(16).padStart(6, '0')}` : textColor;
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText(message, 128, 64);

        const texture = new THREE.CanvasTexture(canvas);
        texture.minFilter = THREE.LinearFilter;
        const sprite = new THREE.Sprite(new THREE.SpriteMaterial({ map: texture, depthTest: false }));
        sprite.renderOrder = 20;
        return sprite;
    }

    /**
     * Dynamic Infinite 2D Grid & Axes for Cartesian Graphing Mode
     * Scales and pans infinitely as the user drags and pinches
     */
    function update2DGridAndAxes() {
        if (!gridHelper || !axesGroup || !camera || !controls) return;
        while (gridHelper.children.length > 0) gridHelper.remove(gridHelper.children[0]);
        while (axesGroup.children.length > 0) axesGroup.remove(axesGroup.children[0]);

        const isDark = construction.view.theme === 'dark';
        const cz = Math.max(2, camera.position.z);
        const viewH = 2 * Math.tan((camera.fov * Math.PI) / 360) * cz;
        const viewW = viewH * camera.aspect;

        const cx = controls.target.x;
        const cy = controls.target.y;

        // Generous bounds buffer covering the viewport completely
        const minX = cx - viewW * 1.2;
        const maxX = cx + viewW * 1.2;
        const minY = cy - viewH * 1.2;
        const maxY = cy + viewH * 1.2;

        // Determine step size (1, 2, 5, 10, 20, ...)
        const rawStep = viewH / 10;
        const power = Math.pow(10, Math.floor(Math.log10(rawStep)));
        const frac = rawStep / power;
        let step = power;
        if (frac >= 5) step = 5 * power;
        else if (frac >= 2) step = 2 * power;
        else step = power;

        const minorStep = step / 5;

        const minorColor = isDark ? 0x1E2638 : 0xF0F2F5;
        const majorColor = isDark ? 0x2A374F : 0xD1D5DB;

        const minorVerts = [];
        const majorVerts = [];

        const startX = Math.floor(minX / minorStep) * minorStep;
        const endX = Math.ceil(maxX / minorStep) * minorStep;
        const startY = Math.floor(minY / minorStep) * minorStep;
        const endY = Math.ceil(maxY / minorStep) * minorStep;

        const eps = minorStep * 0.05;

        for (let x = startX; x <= endX; x += minorStep) {
            const isMajor = Math.abs(x % step) < eps || Math.abs(Math.abs(x % step) - step) < eps;
            if (isMajor) {
                majorVerts.push(x, minY, 0, x, maxY, 0);
            } else {
                minorVerts.push(x, minY, 0, x, maxY, 0);
            }
        }

        for (let y = startY; y <= endY; y += minorStep) {
            const isMajor = Math.abs(y % step) < eps || Math.abs(Math.abs(y % step) - step) < eps;
            if (isMajor) {
                majorVerts.push(minX, y, 0, maxX, y, 0);
            } else {
                minorVerts.push(minX, y, 0, maxX, y, 0);
            }
        }

        if (minorVerts.length > 0) {
            const minorGeom = new THREE.BufferGeometry();
            minorGeom.setAttribute('position', new THREE.Float32BufferAttribute(minorVerts, 3));
            const minorMat = new THREE.LineBasicMaterial({ color: minorColor, transparent: true, opacity: 0.85 });
            gridHelper.add(new THREE.LineSegments(minorGeom, minorMat));
        }

        if (majorVerts.length > 0) {
            const majorGeom = new THREE.BufferGeometry();
            majorGeom.setAttribute('position', new THREE.Float32BufferAttribute(majorVerts, 3));
            const majorMat = new THREE.LineBasicMaterial({ color: majorColor, transparent: true, opacity: 0.95 });
            gridHelper.add(new THREE.LineSegments(majorGeom, majorMat));
        }

        gridHelper.visible = construction.view.showGrid;

        // 2. Cartesian Axes lines spanning edge-to-edge
        const axisColor = isDark ? 0x94A3B8 : 0x1E293B;
        const axisVerts = [
            minX, 0, 0.02, maxX, 0, 0.02,
            0, minY, 0.02, 0, maxY, 0.02
        ];
        const axisGeom = new THREE.BufferGeometry();
        axisGeom.setAttribute('position', new THREE.Float32BufferAttribute(axisVerts, 3));
        const axisMat = new THREE.LineBasicMaterial({ color: axisColor, linewidth: 2.5 });
        axesGroup.add(new THREE.LineSegments(axisGeom, axisMat));

        // 3. Numbers / Ticks on Axes (Large, crisp, prominent on tablet)
        const spriteScale = (viewH / 22) * 1.6;
        const spriteW = spriteScale * 1.1;
        const spriteH = spriteScale * 0.55;
        const tickColor = isDark ? '#E2E8F0' : '#0F172A';

        // X-axis tick numbers
        const numStartX = Math.floor(minX / step) * step;
        const numEndX = Math.ceil(maxX / step) * step;
        const clampedY = Math.max(minY + spriteH, Math.min(maxY - spriteH, -spriteH * 0.75));

        for (let x = numStartX; x <= numEndX; x += step) {
            if (Math.abs(x) < step * 0.2) continue; // skip 0 to prevent overlap
            const labelStr = formatNumberLabel(x, step);
            const sprite = makeTextSprite(labelStr, { textColor: tickColor, fontSize: 50 });
            sprite.scale.set(spriteW, spriteH, 1);
            sprite.position.set(x, clampedY, 0.06);
            axesGroup.add(sprite);
        }

        // Y-axis tick numbers
        const numStartY = Math.floor(minY / step) * step;
        const numEndY = Math.ceil(maxY / step) * step;
        const clampedX = Math.max(minX + spriteW, Math.min(maxX - spriteW, -spriteW * 0.55));

        for (let y = numStartY; y <= numEndY; y += step) {
            if (Math.abs(y) < step * 0.2) continue;
            const labelStr = formatNumberLabel(y, step);
            const sprite = makeTextSprite(labelStr, { textColor: tickColor, fontSize: 50 });
            sprite.scale.set(spriteW, spriteH, 1);
            sprite.position.set(clampedX, y, 0.06);
            axesGroup.add(sprite);
        }

        // Origin label "0"
        const originSprite = makeTextSprite('0', { textColor: tickColor, fontSize: 50 });
        originSprite.scale.set(spriteW * 0.7, spriteH, 1);
        originSprite.position.set(-spriteW * 0.45, -spriteH * 0.75, 0.06);
        axesGroup.add(originSprite);

        // 'x' and 'y' axis names
        const xNameSprite = makeTextSprite('x', { textColor: isDark ? '#38BDF8' : '#0284C7', fontSize: 60 });
        xNameSprite.scale.set(spriteW * 0.85, spriteH * 1.15, 1);
        xNameSprite.position.set(maxX - spriteW * 0.7, clampedY, 0.07);
        axesGroup.add(xNameSprite);

        const yNameSprite = makeTextSprite('y', { textColor: isDark ? '#38BDF8' : '#0284C7', fontSize: 60 });
        yNameSprite.scale.set(spriteW * 0.85, spriteH * 1.15, 1);
        yNameSprite.position.set(clampedX, maxY - spriteH * 0.7, 0.07);
        axesGroup.add(yNameSprite);

        axesGroup.visible = construction.view.showAxes;
    }

    /**
     * Update 2D curves so they span infinitely across the current viewport
     */
    function updateCurvesDomain() {
        if (construction.view.mode !== 'graphing' || !camera || !controls) return;
        const cz = Math.max(2, camera.position.z);
        const viewH = 2 * Math.tan((camera.fov * Math.PI) / 360) * cz;
        const viewW = viewH * camera.aspect;
        const cx = controls.target.x;
        const xMin = cx - viewW * 1.4;
        const xMax = cx + viewW * 1.4;
        const steps = 600;
        const dx = (xMax - xMin) / steps;

        construction.elements.forEach(elem => {
            if (elem.type !== 'FUNCTION_2D' || elem.visible === false || !elem.threeObj) return;
            const expr = elem.expr || 'x^2';
            const fn = compileMath(expr, ['x']);
            if (!fn) return;

            const points = [];
            for (let i = 0; i <= steps; i++) {
                const x = xMin + i * dx;
                const y = fn(x);
                if (!isNaN(y) && isFinite(y) && Math.abs(y) <= 2000) {
                    points.push(new THREE.Vector3(x, y, 0.15));
                }
            }

            const line = elem.threeObj.children.find(c => c.isLine);
            if (line && points.length > 1) {
                line.geometry.dispose();
                line.geometry = new THREE.BufferGeometry().setFromPoints(points);
            }
        });
    }

    /**
     * Build Ground Grid (XY Plane) and Cartesian Axes with Numbers and Ticks
     */
    function buildGridAndAxes() {
        if (construction.view.mode === 'graphing') {
            update2DGridAndAxes();
            return;
        }

        while (gridHelper.children.length > 0) gridHelper.remove(gridHelper.children[0]);
        while (axesGroup.children.length > 0) axesGroup.remove(axesGroup.children[0]);

        const size = 10;
        const isDark = construction.view.theme === 'dark';

        // 1. 3D Ground Grid lines (-10 to 10)
        const minorColor = isDark ? 0x1E2638 : 0xEEEEEE;
        const majorColor = isDark ? 0x2A374F : 0xD8DEE4;

        const minorVerts = [];
        const majorVerts = [];

        for (let i = -size; i <= size; i += 1) {
            if (i % 2 === 0) {
                majorVerts.push(i, 0, -size, i, 0, size);
                majorVerts.push(-size, 0, i, size, 0, i);
            } else {
                minorVerts.push(i, 0, -size, i, 0, size);
                minorVerts.push(-size, 0, i, size, 0, i);
            }
        }

        const minorGeom = new THREE.BufferGeometry();
        minorGeom.setAttribute('position', new THREE.Float32BufferAttribute(minorVerts, 3));
        const minorMat = new THREE.LineBasicMaterial({ color: minorColor, transparent: true, opacity: 0.8 });
        gridHelper.add(new THREE.LineSegments(minorGeom, minorMat));

        const majorGeom = new THREE.BufferGeometry();
        majorGeom.setAttribute('position', new THREE.Float32BufferAttribute(majorVerts, 3));
        const majorMat = new THREE.LineBasicMaterial({ color: majorColor, transparent: true, opacity: 0.9 });
        gridHelper.add(new THREE.LineSegments(majorGeom, majorMat));

        gridHelper.visible = construction.view.showGrid;

        // 2. 3D Axes (X: Red, Y: Green, Z: Blue) with arrows and full range (-10 to 10)
        const axisLength = 10;
        const tickColor = isDark ? '#94A3B8' : '#475569';
        const tickLineColor = isDark ? 0x64748B : 0x94A3B8;

        function makeAxis(dir, colorHex, label) {
            const group = new THREE.Group();

            const totalLen = axisLength * 2;
            const shaftGeom = new THREE.CylinderGeometry(0.045, 0.045, totalLen, 12);
            const defaultDir = new THREE.Vector3(0, 1, 0);
            const quat = new THREE.Quaternion().setFromUnitVectors(defaultDir, dir);

            const mat = new THREE.MeshLambertMaterial({ color: colorHex });
            const shaft = new THREE.Mesh(shaftGeom, mat);
            shaft.quaternion.copy(quat);
            group.add(shaft);

            const coneGeom = new THREE.ConeGeometry(0.28, 0.65, 16);
            coneGeom.translate(0, 0.325, 0);
            const cone = new THREE.Mesh(coneGeom, mat);
            cone.quaternion.copy(quat);
            cone.position.copy(dir.clone().multiplyScalar(axisLength));
            group.add(cone);

            const textSprite = makeTextSprite(label, { textColor: colorHex, fontSize: 52 });
            textSprite.scale.set(1.8, 0.9, 1);
            textSprite.position.copy(dir.clone().multiplyScalar(axisLength + 1.2));
            group.add(textSprite);

            return group;
        }

        // X: Red -> Samping Kanan (1, 0, 0)
        axesGroup.add(makeAxis(new THREE.Vector3(1, 0, 0), COLORS.axisX, 'x'));
        // Y: Green -> Menuju Atas (0, 1, 0)
        axesGroup.add(makeAxis(new THREE.Vector3(0, 1, 0), COLORS.axisY, 'y'));
        // Z: Blue -> Menuju Depan / Forward (0, 0, 1)
        axesGroup.add(makeAxis(new THREE.Vector3(0, 0, 1), COLORS.axisZ, 'z'));

        // 3. Coordinate Values & Ticks for X, Y, Z (every 2 units)
        const tickVerts = [];
        const tickStep = 2;
        const tickSize = 0.28;
        const numColor = isDark ? '#F8FAFC' : '#0F172A';

        for (let v = -size; v <= size; v += tickStep) {
            if (v === 0) continue; // skip 0 to prevent overlap
            const valStr = v.toString();

            // X axis tick & number (runs along Right)
            tickVerts.push(v, 0, -tickSize, v, 0, tickSize);
            const xSprite = makeTextSprite(valStr, { textColor: numColor, fontSize: 50 });
            xSprite.scale.set(1.4, 0.7, 1);
            xSprite.position.set(v, -0.65, 0.65);
            axesGroup.add(xSprite);

            // Y axis tick & number (runs along Up)
            tickVerts.push(-tickSize, v, 0, tickSize, v, 0);
            const ySprite = makeTextSprite(valStr, { textColor: numColor, fontSize: 50 });
            ySprite.scale.set(1.4, 0.7, 1);
            ySprite.position.set(-0.65, v, 0);
            axesGroup.add(ySprite);

            // Z axis tick & number (runs along Forward)
            tickVerts.push(-tickSize, 0, v, tickSize, 0, v);
            const zSprite = makeTextSprite(valStr, { textColor: numColor, fontSize: 50 });
            zSprite.scale.set(1.4, 0.7, 1);
            zSprite.position.set(0.65, -0.65, v);
            axesGroup.add(zSprite);
        }

        const tickGeom = new THREE.BufferGeometry();
        tickGeom.setAttribute('position', new THREE.Float32BufferAttribute(tickVerts, 3));
        const tickMat = new THREE.LineBasicMaterial({ color: tickLineColor, linewidth: 2.2 });
        axesGroup.add(new THREE.LineSegments(tickGeom, tickMat));
        axesGroup.visible = construction.view.showAxes;
    }

    // ==========================================
    // Multi-Object Mesh Builders
    // ==========================================

    /**
     * 1. 2D Curve: y = f(x) (Rendered on XY Plane in 3D Space)
     * Strictly bounded within the 3D grid media (never exceeds media boundaries)
     */
    function buildFunction2D(elem) {
        const group = new THREE.Group();
        const expr = elem.expr || 'x^2';
        const isDualBranch = /y(\^2|\^\{2\}|²|\*y)/i.test(elem.expr || '') ||
                             /y(\^2|\^\{2\}|²|\*y)/i.test(elem.definition || '');
        const fn = compileMath(expr, ['x']);
        if (!fn) return group;

        const mediaBound = 10;
        const xMin = elem.xMin !== undefined ? Math.max(-mediaBound, elem.xMin) : -mediaBound;
        const xMax = elem.xMax !== undefined ? Math.min(mediaBound, elem.xMax) : mediaBound;
        const steps = 600;
        const dx = (xMax - xMin) / steps;

        const curveColor = elem.color ? new THREE.Color(elem.color) : new THREE.Color(0x00E5FF);
        const mat = new THREE.LineBasicMaterial({
            color: curveColor,
            linewidth: 1.5
        });

        function clipAndAddBranch(evalFn, branchMultiplier) {
            const lineSegments = [];
            let prevX = null;
            let prevY = null;

            for (let i = 0; i <= steps; i++) {
                const x = xMin + i * dx;
                let rawY = evalFn(x);
                if (isNaN(rawY) || !isFinite(rawY)) {
                    prevX = null;
                    prevY = null;
                    continue;
                }
                const y = rawY * branchMultiplier;
                const inBounds = Math.abs(y) <= mediaBound;

                if (prevX !== null) {
                    const prevInBounds = Math.abs(prevY) <= mediaBound;
                    if (prevInBounds && inBounds) {
                        lineSegments.push(new THREE.Vector3(prevX, prevY, 0.15));
                        lineSegments.push(new THREE.Vector3(x, y, 0.15));
                    } else if (prevInBounds && !inBounds) {
                        const targetY = y > mediaBound ? mediaBound : -mediaBound;
                        const t = (targetY - prevY) / (y - prevY);
                        const interX = prevX + t * (x - prevX);
                        lineSegments.push(new THREE.Vector3(prevX, prevY, 0.15));
                        lineSegments.push(new THREE.Vector3(interX, targetY, 0.15));
                    } else if (!prevInBounds && inBounds) {
                        const targetY = prevY > mediaBound ? mediaBound : -mediaBound;
                        const t = (targetY - prevY) / (y - prevY);
                        const interX = prevX + t * (x - prevX);
                        lineSegments.push(new THREE.Vector3(interX, targetY, 0.15));
                        lineSegments.push(new THREE.Vector3(x, y, 0.15));
                    }
                }
                prevX = x;
                prevY = y;
            }

            if (lineSegments.length > 0) {
                const geom = new THREE.BufferGeometry().setFromPoints(lineSegments);
                const segObj = new THREE.LineSegments(geom, mat);
                segObj.renderOrder = 10;
                group.add(segObj);
            }
        }

        clipAndAddBranch(fn, 1);
        if (isDualBranch) {
            clipAndAddBranch(fn, -1);
        }

        // GeoNix Curve Label (e.g. 'f', 'g')
        const labelText = elem.name ? elem.name.split(':')[0].trim().replace(/\(x\)/, '') : '';
        if (labelText) {
            for (let i = Math.floor(steps * 0.6); i >= 0; i--) {
                const x = xMin + i * dx;
                const y = fn(x);
                if (!isNaN(y) && isFinite(y) && Math.abs(y) <= mediaBound * 0.85) {
                    const sprite = makeTextSprite(labelText, { textColor: elem.color || '#00E5FF', fontSize: 26 });
                    sprite.scale.set(1.0, 0.5, 1);
                    sprite.renderOrder = 12;
                    sprite.position.set(x + 0.3, y + 0.4, 0.2);
                    group.add(sprite);
                    break;
                }
            }
        }

        // ── 3D DEFINITE INTEGRAL REGION & AREA SHADING (Identik Notes 2D & 3D) ──
        if (elem.hasIntegral) {
            const intA = elem.a !== undefined ? elem.a : 0;
            const intB = elem.b !== undefined ? elem.b : 2;
            const xStart = Math.min(intA, intB);
            const xEnd = Math.max(intA, intB);

            if (xEnd - xStart > 1e-4) {
                const riemannType = elem.integralRiemannType || 'SMOOTH';
                const intZ = 0.05; // Slightly above ground plane to prevent z-fighting

                if (riemannType === 'SMOOTH') {
                    const sampleN = 120;
                    const h = (xEnd - xStart) / sampleN;
                    const areaPositions = [];
                    const areaIndices = [];

                    for (let i = 0; i <= sampleN; i++) {
                        const curX = xStart + i * h;
                        const rawY = fn(curX);
                        const curY = (!isNaN(rawY) && isFinite(rawY)) ? Math.max(-mediaBound, Math.min(mediaBound, rawY)) : 0;

                        // Base point on x-axis
                        areaPositions.push(curX, 0, intZ);
                        // Point on function curve
                        areaPositions.push(curX, curY, intZ);

                        if (i < sampleN) {
                            const p0 = i * 2;
                            const p1 = i * 2 + 1;
                            const p2 = (i + 1) * 2;
                            const p3 = (i + 1) * 2 + 1;
                            areaIndices.push(p0, p1, p2);
                            areaIndices.push(p1, p3, p2);
                        }
                    }

                    const areaGeom = new THREE.BufferGeometry();
                    areaGeom.setAttribute('position', new THREE.Float32BufferAttribute(areaPositions, 3));
                    areaGeom.setIndex(areaIndices);
                    areaGeom.computeVertexNormals();

                    const areaMat = new THREE.MeshBasicMaterial({
                        color: curveColor,
                        transparent: true,
                        opacity: 0.38,
                        side: THREE.DoubleSide,
                        depthWrite: false
                    });
                    const areaMesh = new THREE.Mesh(areaGeom, areaMat);
                    areaMesh.renderOrder = 4;
                    group.add(areaMesh);
                } else {
                    // Riemann sum approximation bars (Trapezoidal, Left, Right, Midpoint)
                    const n = Math.min(60, Math.max(2, elem.integralPartitions || 12));
                    const dxR = (xEnd - xStart) / n;
                    const rPositions = [];
                    const rIndices = [];

                    for (let i = 0; i < n; i++) {
                        const x0 = xStart + i * dxR;
                        const x1 = x0 + dxR;
                        let y0, y1;
                        if (riemannType === 'TRAPEZOIDAL') {
                            y0 = fn(x0);
                            y1 = fn(x1);
                        } else {
                            const evalX = riemannType === 'LEFT' ? x0 : (riemannType === 'RIGHT' ? x1 : (x0 + x1) / 2);
                            const yH = fn(evalX);
                            y0 = yH;
                            y1 = yH;
                        }
                        if (isNaN(y0) || !isFinite(y0)) y0 = 0;
                        if (isNaN(y1) || !isFinite(y1)) y1 = 0;

                        const baseIdx = rPositions.length / 3;
                        rPositions.push(x0, 0, intZ);
                        rPositions.push(x0, y0, intZ);
                        rPositions.push(x1, 0, intZ);
                        rPositions.push(x1, y1, intZ);

                        rIndices.push(baseIdx, baseIdx + 1, baseIdx + 2);
                        rIndices.push(baseIdx + 1, baseIdx + 3, baseIdx + 2);
                    }

                    const rGeom = new THREE.BufferGeometry();
                    rGeom.setAttribute('position', new THREE.Float32BufferAttribute(rPositions, 3));
                    rGeom.setIndex(rIndices);
                    rGeom.computeVertexNormals();

                    const rMat = new THREE.MeshBasicMaterial({
                        color: curveColor,
                        transparent: true,
                        opacity: 0.40,
                        side: THREE.DoubleSide,
                        depthWrite: false
                    });
                    const rMesh = new THREE.Mesh(rGeom, rMat);
                    rMesh.renderOrder = 4;
                    group.add(rMesh);

                    // Outlines for Riemann rectangles
                    const wireMat = new THREE.LineBasicMaterial({ color: curveColor, linewidth: 1.5, transparent: true, opacity: 0.7 });
                    group.add(new THREE.LineSegments(new THREE.WireframeGeometry(rGeom), wireMat));
                }

                // Boundary Dashed/Solid lines at x = intA and x = intB
                function addBoundLine(bx) {
                    const by = fn(bx);
                    const safeY = (!isNaN(by) && isFinite(by)) ? Math.max(-mediaBound, Math.min(mediaBound, by)) : 0;
                    const pts = [new THREE.Vector3(bx, 0, intZ + 0.02), new THREE.Vector3(bx, safeY, intZ + 0.02)];
                    const bGeom = new THREE.BufferGeometry().setFromPoints(pts);
                    const boundLineMat = new THREE.LineBasicMaterial({ color: curveColor, linewidth: 2 });
                    const line = new THREE.Line(bGeom, boundLineMat);
                    line.renderOrder = 11;
                    group.add(line);

                    // Keypoint dot at (bx, by)
                    const dotGeom = new THREE.SphereGeometry(0.12, 16, 16);
                    const dotMat = new THREE.MeshBasicMaterial({ color: 0xFFFFFF });
                    const dot = new THREE.Mesh(dotGeom, dotMat);
                    dot.position.set(bx, safeY, intZ + 0.02);
                    dot.renderOrder = 12;
                    group.add(dot);
                }

                addBoundLine(intA);
                addBoundLine(intB);

                // 3D Evaluated Integral Badge (e.g. ∫ = 16)
                let netInt = 0;
                const simN = 80;
                const simH = (xEnd - xStart) / simN;
                let simSum = 0;
                for (let k = 0; k <= simN; k++) {
                    const sX = xStart + k * simH;
                    const sY = fn(sX);
                    const val = (!isNaN(sY) && isFinite(sY)) ? sY : 0;
                    const w = (k === 0 || k === simN) ? 1 : (k % 2 === 1 ? 4 : 2);
                    simSum += w * val;
                }
                netInt = (simH / 3) * simSum;
                if (intA > intB) netInt = -netInt;

                const midX = (intA + intB) / 2;
                const midY = fn(midX);
                const safeMidY = (!isNaN(midY) && isFinite(midY)) ? midY / 2 : 1;
                const labelStr = "∫ = " + (Math.round(netInt * 100) / 100);
                const badgeSprite = makeTextSprite(labelStr, {
                    textColor: '#FFFFFF',
                    backgroundColor: elem.color || '#2563EB',
                    fontSize: 22
                });
                badgeSprite.scale.set(1.4, 0.6, 1);
                badgeSprite.position.set(midX, safeMidY, intZ + 0.15);
                badgeSprite.renderOrder = 15;
                group.add(badgeSprite);
            }
        }

        return group;
    }

    /**
     * 2. Solids of Revolution (Benda Putar Menggunakan Formula Rodrigues GeoNix)
     * Parameters: expr f(x), interval [a, b], axis ('x' | 'y'), angle theta (0 - 360 deg)
     */
    function buildSolidOfRevolution(elem) {
        const group = new THREE.Group();
        const expr = elem.expr || 'sqrt(x)';
        const fn = compileMath(expr, ['x']);
        if (!fn) return group;

        const a = elem.a !== undefined ? elem.a : 0;
        const b = elem.b !== undefined ? elem.b : 4;
        const axis = (elem.axis || 'x').toLowerCase();
        const angleDeg = elem.angle !== undefined ? Math.max(0.01, Math.min(360, elem.angle)) : 360;
        const maxAngleRad = (angleDeg * Math.PI) / 180;

        const uSubdiv = 50;
        const vSubdiv = 60;
        const du = (b - a) / uSubdiv;
        const dv = maxAngleRad / vSubdiv;

        const positions = [];
        const indices = [];

        // 1. OUTER REVOLVED SURFACE MANTLE (r = f(u))
        for (let i = 0; i <= uSubdiv; i++) {
            const u = a + i * du;
            const fu = fn(u);
            const r = (!isNaN(fu) && isFinite(fu)) ? fu : 0;

            for (let j = 0; j <= vSubdiv; j++) {
                const v = j * dv;
                let x, y, z;
                if (axis === 'x') {
                    x = u;
                    y = r * Math.cos(v);
                    z = r * Math.sin(v);
                } else {
                    x = u * Math.cos(v);
                    y = r;
                    z = u * Math.sin(v);
                }
                positions.push(x, y, z);
            }
        }

        // Triangles for Outer Surface
        for (let i = 0; i < uSubdiv; i++) {
            for (let j = 0; j < vSubdiv; j++) {
                const row1 = i * (vSubdiv + 1);
                const row2 = (i + 1) * (vSubdiv + 1);
                const p1 = row1 + j;
                const p2 = row1 + j + 1;
                const p3 = row2 + j;
                const p4 = row2 + j + 1;
                indices.push(p1, p3, p2);
                indices.push(p2, p3, p4);
            }
        }

        // 2. START CAP AT u = a (Circular Disk / Sector closing the volume)
        const fa = fn(a);
        const ra = (!isNaN(fa) && isFinite(fa)) ? fa : 0;
        if (Math.abs(ra) > 1e-4) {
            const capAStartIdx = positions.length / 3;
            // Center point on axis
            if (axis === 'x') positions.push(a, 0, 0);
            else positions.push(0, ra, 0);

            for (let j = 0; j <= vSubdiv; j++) {
                const v = j * dv;
                if (axis === 'x') positions.push(a, ra * Math.cos(v), ra * Math.sin(v));
                else positions.push(a * Math.cos(v), ra, a * Math.sin(v));
            }

            for (let j = 0; j < vSubdiv; j++) {
                const pCenter = capAStartIdx;
                const p1 = capAStartIdx + 1 + j;
                const p2 = capAStartIdx + 1 + j + 1;
                indices.push(pCenter, p2, p1);
            }
        }

        // 3. END CAP AT u = b (Circular Disk / Sector closing the volume)
        const fb = fn(b);
        const rb = (!isNaN(fb) && isFinite(fb)) ? fb : 0;
        if (Math.abs(rb) > 1e-4) {
            const capBStartIdx = positions.length / 3;
            // Center point on axis
            if (axis === 'x') positions.push(b, 0, 0);
            else positions.push(0, rb, 0);

            for (let j = 0; j <= vSubdiv; j++) {
                const v = j * dv;
                if (axis === 'x') positions.push(b, rb * Math.cos(v), rb * Math.sin(v));
                else positions.push(b * Math.cos(v), rb, b * Math.sin(v));
            }

            for (let j = 0; j < vSubdiv; j++) {
                const pCenter = capBStartIdx;
                const p1 = capBStartIdx + 1 + j;
                const p2 = capBStartIdx + 1 + j + 1;
                indices.push(pCenter, p1, p2);
            }
        }

        // 4. CUT FACES (Longitudinal Cross-sections when angle < 360°)
        // Shows the solid internal 2D area beneath the curve revolving through space!
        if (angleDeg < 359.5) {
            // Cut face at v = 0 (Starting profile plane)
            const cut0StartIdx = positions.length / 3;
            for (let i = 0; i <= uSubdiv; i++) {
                const u = a + i * du;
                const fu = fn(u);
                const r = (!isNaN(fu) && isFinite(fu)) ? fu : 0;
                if (axis === 'x') {
                    positions.push(u, 0, 0); // on axis
                    positions.push(u, r, 0); // on curve
                } else {
                    positions.push(0, r, 0); // on axis
                    positions.push(u, r, 0); // on curve
                }
            }
            for (let i = 0; i < uSubdiv; i++) {
                const p0 = cut0StartIdx + i * 2;
                const p1 = cut0StartIdx + i * 2 + 1;
                const p2 = cut0StartIdx + (i + 1) * 2;
                const p3 = cut0StartIdx + (i + 1) * 2 + 1;
                indices.push(p0, p2, p1);
                indices.push(p1, p2, p3);
            }

            // Cut face at v = maxAngleRad (End profile plane)
            const cutEndStartIdx = positions.length / 3;
            const cosT = Math.cos(maxAngleRad);
            const sinT = Math.sin(maxAngleRad);
            for (let i = 0; i <= uSubdiv; i++) {
                const u = a + i * du;
                const fu = fn(u);
                const r = (!isNaN(fu) && isFinite(fu)) ? fu : 0;
                if (axis === 'x') {
                    positions.push(u, 0, 0); // on axis
                    positions.push(u, r * cosT, r * sinT); // on rotated curve
                } else {
                    positions.push(0, r, 0); // on axis
                    positions.push(u * cosT, r, u * sinT); // on rotated curve
                }
            }
            for (let i = 0; i < uSubdiv; i++) {
                const p0 = cutEndStartIdx + i * 2;
                const p1 = cutEndStartIdx + i * 2 + 1;
                const p2 = cutEndStartIdx + (i + 1) * 2;
                const p3 = cutEndStartIdx + (i + 1) * 2 + 1;
                indices.push(p0, p1, p2);
                indices.push(p1, p3, p2);
            }
        }

        const geom = new THREE.BufferGeometry();
        geom.setAttribute('position', new THREE.Float32BufferAttribute(positions, 3));
        geom.setIndex(indices);
        geom.computeVertexNormals();

        const color = elem.color ? new THREE.Color(elem.color) : new THREE.Color(0x7C3AED);
        const mat = new THREE.MeshPhongMaterial({
            color: color,
            transparent: true,
            opacity: elem.opacity !== undefined ? elem.opacity : 0.82,
            side: THREE.DoubleSide,
            shininess: 60,
            specular: 0x333333
        });
        const mesh = new THREE.Mesh(geom, mat);
        group.add(mesh);

        // Core spine line along rotation axis
        const axisPts = [];
        if (axis === 'x') {
            axisPts.push(new THREE.Vector3(a, 0, 0), new THREE.Vector3(b, 0, 0));
        } else {
            axisPts.push(new THREE.Vector3(0, Math.min(a, b), 0), new THREE.Vector3(0, Math.max(a, b), 0));
        }
        const axisGeom = new THREE.BufferGeometry().setFromPoints(axisPts);
        const axisLineMat = new THREE.LineBasicMaterial({ color: 0xFFD700, linewidth: 2.5 });
        group.add(new THREE.Line(axisGeom, axisLineMat));

        // Edge wireframe accents on the solid body
        const wireMat = new THREE.LineBasicMaterial({
            color: 0xFFFFFF,
            transparent: true,
            opacity: 0.25
        });
        group.add(new THREE.LineSegments(new THREE.WireframeGeometry(geom), wireMat));

        // Compute Volume using Simpson's Rule
        let volume = 0;
        const simpsonN = 100;
        const h = (b - a) / simpsonN;
        let sum = 0;
        if (axis === 'x') {
            for (let k = 0; k <= simpsonN; k++) {
                const xVal = a + k * h;
                const yVal = fn(xVal);
                const fx2 = (!isNaN(yVal) && isFinite(yVal)) ? yVal * yVal : 0;
                const weight = (k === 0 || k === simpsonN) ? 1 : (k % 2 === 1 ? 4 : 2);
                sum += weight * fx2;
            }
            volume = (Math.PI * (h / 3) * sum) * (angleDeg / 360);
        } else {
            for (let k = 0; k <= simpsonN; k++) {
                const xVal = a + k * h;
                const yVal = fn(xVal);
                const xfx = (!isNaN(yVal) && isFinite(yVal)) ? Math.abs(xVal * yVal) : 0;
                const weight = (k === 0 || k === simpsonN) ? 1 : (k % 2 === 1 ? 4 : 2);
                sum += weight * xfx;
            }
            volume = (2 * Math.PI * (h / 3) * sum) * (angleDeg / 360);
        }
        elem.metrics = { volume: volume };

        return group;
    }

    /**
     * 3. 3D Multivariable Surface: z = f(x, y)
     */
    function buildSurface3D(elem) {
        const group = new THREE.Group();
        const expr = elem.expr || 'sin(x)*cos(y)';
        const fn = compileMath(expr, ['x', 'y']);
        if (!fn) return group;

        const range = elem.range || 5;
        const subdiv = 50;
        const step = (2 * range) / subdiv;

        const positions = [];
        const colors = [];
        const indices = [];

        for (let i = 0; i <= subdiv; i++) {
            const x = -range + i * step;
            for (let j = 0; j <= subdiv; j++) {
                const y = -range + j * step;
                let z = fn(x, y);
                if (isNaN(z) || !isFinite(z)) z = 0;
                positions.push(x, y, z);

                // Elevation color gradient
                const normZ = (z + 3) / 6;
                const c = new THREE.Color().setHSL(0.65 - 0.5 * Math.min(Math.max(normZ, 0), 1), 0.9, 0.55);
                colors.push(c.r, c.g, c.b);
            }
        }

        for (let i = 0; i < subdiv; i++) {
            for (let j = 0; j < subdiv; j++) {
                const row1 = i * (subdiv + 1);
                const row2 = (i + 1) * (subdiv + 1);
                indices.push(row1 + j, row2 + j, row1 + j + 1);
                indices.push(row1 + j + 1, row2 + j, row2 + j + 1);
            }
        }

        const geom = new THREE.BufferGeometry();
        geom.setAttribute('position', new THREE.Float32BufferAttribute(positions, 3));
        geom.setAttribute('color', new THREE.Float32BufferAttribute(colors, 3));
        geom.setIndex(indices);
        geom.computeVertexNormals();

        const mat = new THREE.MeshPhongMaterial({
            vertexColors: true,
            side: THREE.DoubleSide,
            transparent: true,
            opacity: elem.opacity !== undefined ? elem.opacity : 0.85,
            shininess: 40
        });
        group.add(new THREE.Mesh(geom, mat));
        return group;
    }

    /**
     * 4. 3D Vector: v = <x, y, z>
     */
    function buildVector(elem) {
        const group = new THREE.Group();
        const x = elem.x !== undefined ? elem.x : 2;
        const y = elem.y !== undefined ? elem.y : 3;
        const z = elem.z !== undefined ? elem.z : 4;
        const start = elem.start ? new THREE.Vector3(elem.start.x, elem.start.y, elem.start.z) : new THREE.Vector3(0, 0, 0);
        const end = new THREE.Vector3(x, y, z);

        const dir = new THREE.Vector3().subVectors(end, start);
        const len = dir.length();
        if (len < 0.001) return group;
        dir.normalize();

        let headLen = 0.6;
        if (headLen > len * 0.85) headLen = len * 0.85;
        const shaftLen = len - headLen;

        const colorHex = elem.color ? new THREE.Color(elem.color) : 0x00E5FF;
        const mat = new THREE.MeshLambertMaterial({ color: colorHex });

        if (shaftLen > 0.001) {
            const shaftGeom = new THREE.CylinderGeometry(0.05, 0.05, shaftLen, 12);
            shaftGeom.translate(0, shaftLen / 2, 0);
            shaftGeom.rotateX(Math.PI / 2);

            const rotMatrix = new THREE.Matrix4();
            const defaultDir = new THREE.Vector3(0, 0, 1);
            if (Math.abs(defaultDir.dot(dir)) > 0.999) {
                if (dir.z < 0) rotMatrix.makeRotationX(Math.PI);
            } else {
                const axis = new THREE.Vector3().crossVectors(defaultDir, dir).normalize();
                rotMatrix.makeRotationAxis(axis, Math.acos(defaultDir.dot(dir)));
            }
            const shaft = new THREE.Mesh(shaftGeom, mat);
            shaft.applyMatrix4(rotMatrix);
            shaft.position.copy(start);
            group.add(shaft);

            const coneGeom = new THREE.ConeGeometry(0.18, headLen, 16);
            coneGeom.translate(0, headLen / 2, 0);
            coneGeom.rotateX(Math.PI / 2);
            const cone = new THREE.Mesh(coneGeom, mat);
            cone.applyMatrix4(rotMatrix);
            cone.position.copy(start.clone().add(dir.clone().multiplyScalar(shaftLen)));
            group.add(cone);
        }

        if (elem.label) {
            const label = makeTextSprite(elem.label, { textColor: colorHex, fontSize: 24 });
            label.position.copy(end.clone().add(dir.clone().multiplyScalar(0.4)));
            group.add(label);
        }

        elem.metrics = { magnitude: len };
        return group;
    }

    /**
     * 5. 3D Point: P = (x, y, z)
     */
    function buildPoint(elem) {
        const group = new THREE.Group();
        const x = elem.x || 0;
        const y = elem.y || 0;
        const z = elem.z || 0;

        const geom = new THREE.SphereGeometry(0.18, 16, 16);
        const mat = new THREE.MeshLambertMaterial({ color: elem.color ? new THREE.Color(elem.color) : 0xFFEB3B });
        const sphere = new THREE.Mesh(geom, mat);
        sphere.position.set(x, y, z);
        group.add(sphere);

        if (elem.label) {
            const label = makeTextSprite(`${elem.label}(${x},${y},${z})`, {
                textColor: elem.color || '#FFEB3B',
                fontSize: 22
            });
            label.position.set(x, y, z + 0.4);
            group.add(label);
        }
        return group;
    }

    /**
     * 6. 3D Plane: ax + by + cz = d
     */
    function buildPlane(elem) {
        const group = new THREE.Group();
        const a = elem.a !== undefined ? elem.a : 1;
        const b = elem.b !== undefined ? elem.b : 1;
        const c = elem.c !== undefined ? elem.c : 1;
        const d = elem.d !== undefined ? elem.d : 2;

        const normal = new THREE.Vector3(a, b, c);
        const len = normal.length();
        if (len < 0.001) return group;
        normal.normalize();

        // Point on plane closest to origin
        const p0 = normal.clone().multiplyScalar(d / len);

        // Plane Quad (12x12)
        const planeGeom = new THREE.PlaneGeometry(12, 12);
        const color = elem.color ? new THREE.Color(elem.color) : 0x00E676;
        const mat = new THREE.MeshPhongMaterial({
            color: color,
            transparent: true,
            opacity: elem.opacity !== undefined ? elem.opacity : 0.4,
            side: THREE.DoubleSide
        });
        const mesh = new THREE.Mesh(planeGeom, mat);
        mesh.quaternion.setFromUnitVectors(new THREE.Vector3(0, 0, 1), normal);
        mesh.position.copy(p0);
        group.add(mesh);

        return group;
    }

    /**
     * 7. 3D Polyhedron / Solids (Cube, Sphere, Cylinder, Cone, Prism, Pyramid)
     */
    function buildPolyhedron(elem) {
        const group = new THREE.Group();
        const type = (elem.subType || 'CUBE').toUpperCase();
        let geom;
        const s = elem.size || 4;
        const h = elem.height || 5;
        const r = elem.radius || 2.5;

        switch (type) {
            case 'CUBE':
                geom = new THREE.BoxGeometry(s, s, s);
                geom.translate(0, s / 2, 0);
                break;
            case 'CUBOID':
                geom = new THREE.BoxGeometry(elem.width || 4, elem.depth || 3, h);
                geom.translate(0, h / 2, 0);
                break;
            case 'SPHERE':
                geom = new THREE.SphereGeometry(r, 32, 24);
                geom.translate(0, r, 0);
                break;
            case 'CYLINDER':
                geom = new THREE.CylinderGeometry(r, r, h, 32);
// Cylinder and cone are naturally Y-up
                geom.translate(0, h / 2, 0);
                break;
            case 'CONE':
                geom = new THREE.ConeGeometry(r, h, 32);
// Cylinder and cone are naturally Y-up
                geom.translate(0, h / 2, 0);
                break;
            default:
                geom = new THREE.BoxGeometry(s, s, s);
                geom.translate(0, s / 2, 0);
        }

        const color = elem.color ? new THREE.Color(elem.color) : 0x3F51B5;
        const mat = new THREE.MeshPhongMaterial({
            color: color,
            transparent: true,
            opacity: elem.opacity !== undefined ? elem.opacity : 0.75,
            side: THREE.DoubleSide
        });
        group.add(new THREE.Mesh(geom, mat));

        // Wireframe edges
        const edges = new THREE.EdgesGeometry(geom, 25);
        const lineMat = new THREE.LineBasicMaterial({ color: 0xFFFFFF, opacity: 0.8, transparent: true });
        group.add(new THREE.LineSegments(edges, lineMat));

        if (elem.position) {
            group.position.set(elem.position.x || 0, elem.position.y || 0, elem.position.z || 0);
        }
        return group;
    }

    /**
     * Re-render all elements currently in the construction map
     */
    function renderConstruction() {
        while (objectsRootGroup.children.length > 0) {
            objectsRootGroup.remove(objectsRootGroup.children[0]);
        }

        const metricsSummary = [];
        const is2D = construction.view.mode === 'graphing';

        construction.elements.forEach(elem => {
            if (elem.visible === false) return;
            // In 2D Cartesian graphing mode, ONLY show 2D ordinary graph functions & points
            if (is2D && elem.type !== 'FUNCTION_2D' && elem.type !== 'POINT_3D') return;

            let objGroup = null;
            switch (elem.type) {
                case 'FUNCTION_2D':
                    objGroup = buildFunction2D(elem);
                    break;
                case 'SOLID_OF_REVOLUTION':
                    objGroup = buildSolidOfRevolution(elem);
                    break;
                case 'SURFACE_3D':
                    objGroup = buildSurface3D(elem);
                    break;
                case 'VECTOR_3D':
                    objGroup = buildVector(elem);
                    break;
                case 'POINT_3D':
                    objGroup = buildPoint(elem);
                    break;
                case 'PLANE_3D':
                    objGroup = buildPlane(elem);
                    break;
                case 'POLYHEDRON':
                    objGroup = buildPolyhedron(elem);
                    break;
            }

            if (objGroup) {
                elem.threeObj = objGroup;
                objectsRootGroup.add(objGroup);
            }

            if (elem.metrics) {
                metricsSummary.push({ id: elem.id, type: elem.type, metrics: elem.metrics });
            }
        });

        notifyBridge('onElementsUpdated', metricsSummary);
    }

    function animate() {
        requestAnimationFrame(animate);
        controls.update();
        renderer.render(scene, camera);
    }

    function onWindowResize() {
        const container = document.getElementById('viewport');
        const w = container.clientWidth || window.innerWidth;
        const h = container.clientHeight || window.innerHeight;
        camera.aspect = w / h;
        camera.updateProjectionMatrix();
        renderer.setSize(w, h);
    }

    function notifyBridge(methodName, payload) {
        if (window.LernspaceBridge && typeof window.LernspaceBridge[methodName] === 'function') {
            try {
                window.LernspaceBridge[methodName](JSON.stringify(payload));
            } catch (e) {
                console.error(`Error notifying bridge ${methodName}:`, e);
            }
        }
    }

    // ==========================================
    // Public API for Kotlin Bridge (evaluateJavascript)
    // ==========================================

    window.Math3D.setElements = function (elementsJson) {
        try {
            const list = typeof elementsJson === 'string' ? JSON.parse(elementsJson) : elementsJson;
            construction.elements.clear();
            list.forEach(item => {
                construction.elements.set(item.id, item);
            });
            renderConstruction();
        } catch (e) {
            console.error('Failed to set elements:', e);
        }
    };

    window.Math3D.setVariable = function (name, value) {
        construction.variables.set(name, parseFloat(value));
        renderConstruction();
    };

    window.Math3D.setCameraView = function (preset) {
        switch (preset) {
            case 'front':
                camera.position.set(0, 0, 22);
                camera.up.set(0, 1, 0);
                controls.target.set(0, 0, 0);
                break;
            case 'top':
                camera.position.set(0, 22, 0);
                camera.up.set(0, 0, -1);
                controls.target.set(0, 0, 0);
                break;
            case 'side':
                camera.position.set(22, 0, 0);
                camera.up.set(0, 1, 0);
                controls.target.set(0, 0, 0);
                break;
            case 'iso':
            default:
                camera.position.set(14, 12, 18);
                camera.up.set(0, 1, 0);
                controls.target.set(0, 0, 0);
                break;
        }
        controls.update();
    };

    window.Math3D.captureSnapshot = function () {
        renderer.render(scene, camera);
        const dataUrl = renderer.domElement.toDataURL('image/png');
        notifyBridge('onSnapshotCaptured', { dataUrl: dataUrl });
    };

    window.Math3D.zoom = function (factor) {
        if (!camera || !controls) return;
        const offset = new THREE.Vector3().subVectors(camera.position, controls.target);
        let dist = offset.length() * factor;
        const minD = controls.minDistance || 3.5;
        const maxD = controls.maxDistance || 45.0;
        dist = Math.max(minD, Math.min(maxD, dist));
        offset.setLength(dist);
        camera.position.copy(controls.target).add(offset);
        controls.update();
    };

    window.Math3D.setMode = function (mode) {
        construction.view.mode = mode; // 'graphing' (2D) | '3d'
        lastCamX = null; lastCamY = null; lastCamZ = null;
        if (controls) {
            if (mode === 'graphing') {
                controls.enableRotate = false;
                controls.screenSpacePanning = true;
                controls.touches.ONE = THREE.TOUCH.PAN; // 1-finger = PAN (geser-geser)
                controls.touches.TWO = THREE.TOUCH.DOLLY_PAN; // pinch = ZOOM & PAN
                controls.mouseButtons.LEFT = THREE.MOUSE.PAN;
                controls.minDistance = 2.0;
                controls.maxDistance = 150.0;
                window.Math3D.setCameraView('top');
            } else {
                controls.enableRotate = true;
                controls.minPolarAngle = 0.04;
                controls.maxPolarAngle = Math.PI - 0.04;
                controls.screenSpacePanning = false;
                controls.touches.ONE = THREE.TOUCH.ROTATE; // 1-finger = ROTATE in 3D
                controls.touches.TWO = THREE.TOUCH.DOLLY_ROTATE; // 2-finger = Pinch Zoom & Tilt/Pitch
                controls.mouseButtons.LEFT = THREE.MOUSE.ROTATE;
                controls.minDistance = 3.5;
                controls.maxDistance = 45.0;
                window.Math3D.setCameraView('iso');
            }
        }
        buildGridAndAxes();
        renderConstruction();
    };

    window.Math3D.toggleGrid = function (show) {
        construction.view.showGrid = (show !== undefined) ? show : !construction.view.showGrid;
        if (gridHelper) gridHelper.visible = construction.view.showGrid;
    };

    window.Math3D.toggleAxes = function (show) {
        construction.view.showAxes = (show !== undefined) ? show : !construction.view.showAxes;
        if (axesGroup) axesGroup.visible = construction.view.showAxes;
    };

    window.Math3D.setTheme = function (theme) {
        construction.view.theme = theme;
        if (scene) scene.background = new THREE.Color(theme === 'dark' ? 0x0F131C : 0xFFFFFF);
        buildGridAndAxes();
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
