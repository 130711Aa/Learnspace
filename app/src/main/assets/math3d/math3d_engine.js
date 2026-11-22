/**
 * Lernspace Math 3D Lab - GeoGebra-Grade Multi-Object Construction Engine
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
            theme: 'dark' // 'dark' | 'light'
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
     * Mathematical Formula Compiler
     * Converts math expressions like "x^2 + 2x - sin(y)" into fast JS functions.
     */
    function compileMath(exprStr, varNames) {
        if (!exprStr || typeof exprStr !== 'string') return null;
        let clean = exprStr.trim()
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
            console.warn('Failed to compile expression:', exprStr, e);
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
        scene.background = new THREE.Color(construction.view.theme === 'dark' ? 0x0F131C : 0xF8F9FA);

        // Z-up coordinate system (standard GeoGebra)
        camera = new THREE.PerspectiveCamera(45, width / height, 0.1, 1000);
        camera.up.set(0, 0, 1);
        camera.position.set(12, -15, 10);
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
        controls.dampingFactor = 0.08;
        controls.rotateSpeed = 0.8;
        controls.zoomSpeed = 1.0;
        controls.panSpeed = 0.8;

        // Lighting (Ambient + Two Directional Lights)
        const ambientLight = new THREE.AmbientLight(0xFFFFFF, 0.7);
        scene.add(ambientLight);

        const dirLight1 = new THREE.DirectionalLight(0xFFFFFF, 0.7);
        dirLight1.position.set(15, -12, 20);
        scene.add(dirLight1);

        const dirLight2 = new THREE.DirectionalLight(0xB0BEC5, 0.4);
        dirLight2.position.set(-15, 12, -10);
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

    /**
     * Build Ground Grid (XY Plane) and Cartesian Axes
     */
    function buildGridAndAxes() {
        while (gridHelper.children.length > 0) gridHelper.remove(gridHelper.children[0]);
        while (axesGroup.children.length > 0) axesGroup.remove(axesGroup.children[0]);

        const size = 16;
        const step = 1;
        const gridColor = construction.view.theme === 'dark' ? 0x222C3D : 0xE0E0E0;

        const gridGeometry = new THREE.BufferGeometry();
        const gridVertices = [];
        for (let i = -size; i <= size; i += step) {
            gridVertices.push(i, -size, 0, i, size, 0);
            gridVertices.push(-size, i, 0, size, i, 0);
        }
        gridGeometry.setAttribute('position', new THREE.Float32BufferAttribute(gridVertices, 3));
        const gridMaterial = new THREE.LineBasicMaterial({
            color: gridColor,
            transparent: true,
            opacity: 0.85
        });
        gridHelper.add(new THREE.LineSegments(gridGeometry, gridMaterial));
        gridHelper.visible = construction.view.showGrid;

        // Axes (X: Red, Y: Green, Z: Blue)
        const axisLength = 10;
        function makeAxis(dir, colorHex, label) {
            const group = new THREE.Group();
            const shaftLen = axisLength - 0.7;
            const shaftGeom = new THREE.CylinderGeometry(0.04, 0.04, shaftLen, 12);
            shaftGeom.translate(0, shaftLen / 2, 0);
            shaftGeom.rotateX(Math.PI / 2);

            const rotMatrix = new THREE.Matrix4();
            const defaultUp = new THREE.Vector3(0, 0, 1);
            if (Math.abs(defaultUp.dot(dir)) > 0.999) {
                if (dir.z < 0) rotMatrix.makeRotationX(Math.PI);
            } else {
                const axis = new THREE.Vector3().crossVectors(defaultUp, dir).normalize();
                rotMatrix.makeRotationAxis(axis, Math.acos(defaultUp.dot(dir)));
            }
            const mat = new THREE.MeshLambertMaterial({ color: colorHex });
            const shaft = new THREE.Mesh(shaftGeom, mat);
            shaft.applyMatrix4(rotMatrix);
            group.add(shaft);

            const coneGeom = new THREE.ConeGeometry(0.3, 0.7, 16);
            coneGeom.translate(0, 0.35, 0);
            coneGeom.rotateX(Math.PI / 2);
            const cone = new THREE.Mesh(coneGeom, mat);
            cone.applyMatrix4(rotMatrix);
            cone.position.copy(dir.clone().multiplyScalar(shaftLen));
            group.add(cone);

            const textSprite = makeTextSprite(label, { textColor: colorHex, fontSize: 26 });
            textSprite.position.copy(dir.clone().multiplyScalar(axisLength + 0.6));
            group.add(textSprite);

            return group;
        }

        axesGroup.add(makeAxis(new THREE.Vector3(1, 0, 0), COLORS.axisX, 'X'));
        axesGroup.add(makeAxis(new THREE.Vector3(0, 1, 0), COLORS.axisY, 'Y'));
        axesGroup.add(makeAxis(new THREE.Vector3(0, 0, 1), COLORS.axisZ, 'Z'));
        axesGroup.visible = construction.view.showAxes;
    }

    function makeTextSprite(message, opts = {}) {
        const textColor = opts.textColor || 0xFFFFFF;
        const fontSize = opts.fontSize || 24;
        const canvas = document.createElement('canvas');
        canvas.width = 128;
        canvas.height = 64;
        const ctx = canvas.getContext('2d');
        ctx.font = `bold ${fontSize}px Inter, sans-serif`;
        ctx.fillStyle = typeof textColor === 'number' ? `#${textColor.toString(16).padStart(6, '0')}` : textColor;
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText(message, 64, 32);

        const texture = new THREE.CanvasTexture(canvas);
        texture.minFilter = THREE.LinearFilter;
        const sprite = new THREE.Sprite(new THREE.SpriteMaterial({ map: texture, depthTest: false }));
        sprite.scale.set(1.5, 0.75, 1);
        return sprite;
    }

    // ==========================================
    // Multi-Object Mesh Builders
    // ==========================================

    /**
     * 1. 2D Curve: y = f(x) (Rendered on XY Plane in 3D Space)
     */
    function buildFunction2D(elem) {
        const group = new THREE.Group();
        const expr = elem.expr || 'x^2';
        const fn = compileMath(expr, ['x']);
        if (!fn) return group;

        const xMin = elem.xMin !== undefined ? elem.xMin : -8;
        const xMax = elem.xMax !== undefined ? elem.xMax : 8;
        const steps = 300;
        const dx = (xMax - xMin) / steps;

        const points = [];
        for (let i = 0; i <= steps; i++) {
            const x = xMin + i * dx;
            const y = fn(x);
            if (!isNaN(y) && isFinite(y) && Math.abs(y) <= 50) {
                points.push(new THREE.Vector3(x, y, 0));
            }
        }

        if (points.length > 1) {
            const geom = new THREE.BufferGeometry().setFromPoints(points);
            const mat = new THREE.LineBasicMaterial({
                color: elem.color ? new THREE.Color(elem.color) : 0x00E5FF,
                linewidth: 2.5
            });
            group.add(new THREE.Line(geom, mat));
        }
        return group;
    }

    /**
     * 2. Solids of Revolution (Benda Putar Menggunakan Formula Rodrigues GeoGebra)
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
        const angleDeg = elem.angle !== undefined ? elem.angle : 360;
        const maxAngleRad = (angleDeg * Math.PI) / 180;

        const uSubdiv = 50;
        const vSubdiv = 60;
        const du = (b - a) / uSubdiv;
        const dv = maxAngleRad / vSubdiv;

        const positions = [];
        const indices = [];

        // Grid vertex points
        for (let i = 0; i <= uSubdiv; i++) {
            const u = a + i * du;
            const fu = fn(u);
            const r = (!isNaN(fu) && isFinite(fu)) ? fu : 0;

            for (let j = 0; j <= vSubdiv; j++) {
                const v = j * dv;
                let x, y, z;

                if (axis === 'x') {
                    // Revolution around X-axis: X = u, Y = r * cos(v), Z = r * sin(v)
                    x = u;
                    y = r * Math.cos(v);
                    z = r * Math.sin(v);
                } else {
                    // Revolution around Y-axis: X = u * cos(v), Y = u * sin(v), Z = r
                    x = u * Math.cos(v);
                    y = u * Math.sin(v);
                    z = r;
                }
                positions.push(x, y, z);
            }
        }

        // Triangulation (2 triangles per quad)
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

        const geom = new THREE.BufferGeometry();
        geom.setAttribute('position', new THREE.Float32BufferAttribute(positions, 3));
        geom.setIndex(indices);
        geom.computeVertexNormals();

        const color = elem.color ? new THREE.Color(elem.color) : 0x7C3AED;
        const mat = new THREE.MeshPhongMaterial({
            color: color,
            transparent: true,
            opacity: elem.opacity !== undefined ? elem.opacity : 0.75,
            side: THREE.DoubleSide,
            shininess: 45
        });
        const mesh = new THREE.Mesh(geom, mat);
        group.add(mesh);

        // Optional wireframe outlines
        const wireMat = new THREE.LineBasicMaterial({
            color: 0xFFFFFF,
            transparent: true,
            opacity: 0.35
        });
        group.add(new THREE.LineSegments(new THREE.WireframeGeometry(geom), wireMat));

        // Compute Volume using Simpson's Rule
        let volume = 0;
        const simpsonN = 100;
        const h = (b - a) / simpsonN;
        let sum = 0;
        for (let k = 0; k <= simpsonN; k++) {
            const xVal = a + k * h;
            const yVal = fn(xVal);
            const fx2 = (!isNaN(yVal) && isFinite(yVal)) ? yVal * yVal : 0;
            const weight = (k === 0 || k === simpsonN) ? 1 : (k % 2 === 1 ? 4 : 2);
            sum += weight * fx2;
        }
        volume = (Math.PI * (h / 3) * sum) * (angleDeg / 360);
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
                geom.translate(0, 0, s / 2);
                break;
            case 'CUBOID':
                geom = new THREE.BoxGeometry(elem.width || 4, elem.depth || 3, h);
                geom.translate(0, 0, h / 2);
                break;
            case 'SPHERE':
                geom = new THREE.SphereGeometry(r, 32, 24);
                geom.translate(0, 0, r);
                break;
            case 'CYLINDER':
                geom = new THREE.CylinderGeometry(r, r, h, 32);
                geom.rotateX(Math.PI / 2);
                geom.translate(0, 0, h / 2);
                break;
            case 'CONE':
                geom = new THREE.ConeGeometry(r, h, 32);
                geom.rotateX(Math.PI / 2);
                geom.translate(0, 0, h / 2);
                break;
            default:
                geom = new THREE.BoxGeometry(s, s, s);
                geom.translate(0, 0, s / 2);
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

        construction.elements.forEach(elem => {
            if (elem.visible === false) return;

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
            case 'top':
                camera.position.set(0, 0, 22);
                camera.up.set(0, 1, 0);
                controls.target.set(0, 0, 0);
                break;
            case 'front':
                camera.position.set(0, -22, 2);
                camera.up.set(0, 0, 1);
                controls.target.set(0, 0, 2);
                break;
            case 'side':
                camera.position.set(22, 0, 2);
                camera.up.set(0, 0, 1);
                controls.target.set(0, 0, 2);
                break;
            case 'iso':
            default:
                camera.position.set(12, -15, 10);
                camera.up.set(0, 0, 1);
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

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
