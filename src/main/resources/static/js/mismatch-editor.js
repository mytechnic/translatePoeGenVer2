// js/mismatch-editor.js

document.addEventListener('DOMContentLoaded', function () {
    const fileSelect = document.getElementById('fileSelect');
    const labelSelect = document.getElementById('labelSelect');
    const fieldSelect = document.getElementById('fieldSelect');
    const dictionaryFileNameInput = document.getElementById('dictionaryFileNameInput');
    const listContainer = document.getElementById('listContainer');
    const saveButton = document.getElementById('saveButton');

    let mismatchData = [];
    let selectedFile = '';
    let selectedLabel = '';

    function fetchFiles() {
        fetch('/api/mismatch/load')
            .then(res => res.json())
            .then(files => {
                fileSelect.innerHTML = files.map(f => `<option value="${f}">${f}</option>`).join('');
                if (files.length > 0) {
                    selectedFile = files[0];
                    autoSetDictionaryFileName(selectedFile);
                    fetchData(selectedFile);
                }
            }).catch(error => {
            alert('파일을 로드하는 중 오류가 발생했습니다.');
            console.error(error);
        });
    }

    function autoSetDictionaryFileName(fileName) {
        if (fileName.startsWith('mismatch')) {
            dictionaryFileNameInput.value = fileName.replace(/^mismatch/, 'dic');
        } else {
            dictionaryFileNameInput.value = '';
        }
    }

    function fetchData(fileName) {
        fetch(`/api/mismatch/get?fileName=${encodeURIComponent(fileName)}`)
            .then(res => res.json())
            .then(data => {
                mismatchData = data || [];
                updateLabelSelect();
            }).catch(error => {
            alert('데이터를 로드하는 중 오류가 발생했습니다.');
            console.error(error);
        });
    }

    function updateLabelSelect() {
        const labels = [...new Set(mismatchData.map(d => d.label))];
        labelSelect.innerHTML = labels.map(l => `<option value="${l}">${l}</option>`).join('');

        if (labels.length > 0) {
            selectedLabel = labels[0];
            labelSelect.value = selectedLabel;
            renderEntries();
        }
    }

    function renderEntries() {
        const target = mismatchData.find(d => d.label === selectedLabel);
        if (!target) {
            listContainer.innerHTML = '';
            return;
        }

        const selectedField = fieldSelect.value;
        const engRaw = target.entries?.engEntry?.[selectedField] || [];
        const korRaw = target.entries?.korEntry?.[selectedField] || [];

        // 영어, 한글 배열 각각 필터링, 빈값을 포함하여 처리
        const engList = engRaw.concat(); // 복사본 생성
        const korList = korRaw.concat();

        const max = Math.max(engList.length, korList.length);

        let html = `
            <div class="title-row">
                <div class="field-name">항목</div>
                <div class="input-eng">영어</div>
                <div class="input-kor">한글</div>
            </div>`;

        // 영어/한글 배열 독립적으로 출력
        for (let i = 0; i < max; i++) {
            const engValue = engList[i] || ''; // 빈 값도 처리
            const korValue = korList[i] || ''; // 빈 값도 처리

            html += `
            <div class="data-row bg-${selectedField}">
                <div class="field-name">${selectedField}</div>
                <input class="input-eng" data-index="${i}" value="${engValue}">
                <input class="input-kor" data-index="${i}" value="${korValue}">
                <button class="push-eng" data-index="${i}">+영어</button>
                <button class="pull-eng" data-index="${i}">↥영어</button>
                <button class="push-kor" data-index="${i}">+한글</button>
                <button class="pull-kor" data-index="${i}">↥한글</button>
            </div>`;
        }

        listContainer.innerHTML = html;

        bindMoveButtons(engList, korList); // 버튼 바인딩
    }

    function bindMoveButtons(engList, korList) {
        console.log('이동 버튼 바인딩 시작');

        // 영어 항목 추가
        document.querySelectorAll('.push-eng').forEach(btn => {
            btn.addEventListener('click', () => {
                const idx = parseInt(btn.dataset.index);
                if (engList.length < korList.length) {
                    engList.splice(idx, 0, ''); // 빈 값을 넣어줌
                    renderFromModified(engList, korList);
                }
            });
        });

        // 영어 항목 삭제
        document.querySelectorAll('.pull-eng').forEach(btn => {
            btn.addEventListener('click', () => {
                const idx = parseInt(btn.dataset.index);
                if (engList[idx] === '' && idx < engList.length - 1) {
                    engList.splice(idx, 1);
                    renderFromModified(engList, korList);
                }
            });
        });

        // 한글 항목 추가
        document.querySelectorAll('.push-kor').forEach(btn => {
            btn.addEventListener('click', () => {
                const idx = parseInt(btn.dataset.index);
                korList.splice(idx, 0, ''); // 한글 값도 빈 문자열로 추가
                renderFromModified(engList, korList); // 수정된 배열로 렌더링
            });
        });

        // 한글 항목 삭제
        document.querySelectorAll('.pull-kor').forEach(btn => {
            btn.addEventListener('click', () => {
                const idx = parseInt(btn.dataset.index);
                if (korList[idx] === '' && idx < korList.length - 1) {
                    korList.splice(idx, 1);
                    renderFromModified(engList, korList); // 수정된 배열로 렌더링
                }
            });
        });
    }

    function renderFromModified(engList, korList) {
        const target = mismatchData.find(d => d.label === selectedLabel);
        const selectedField = fieldSelect.value;
        if (target) {
            target.entries.engEntry[selectedField] = engList;
            target.entries.korEntry[selectedField] = korList;
            renderEntries(); // 변경된 리스트를 기반으로 다시 렌더링
        }
    }

    saveButton.addEventListener('click', function () {
        const dictionaryFileName = dictionaryFileNameInput.value.trim();
        if (!dictionaryFileName) {
            alert('저장할 파일명을 입력하세요.');
            return;
        }

        const inputs = listContainer.querySelectorAll('input');
        const mappings = {};

        for (let i = 0; i < inputs.length; i += 2) {
            const eng = inputs[i]?.value?.trim();
            const kor = inputs[i + 1]?.value?.trim();
            if (eng && kor) {
                mappings[eng.toLowerCase()] = kor;
            }
        }

        if (Object.keys(mappings).length === 0) {
            alert('저장할 데이터가 없습니다.');
            return;
        }

        const body = {
            label: selectedLabel,
            mismatchFileName: selectedFile,
            dictionaryFileName: dictionaryFileName,
            field: fieldSelect.value,
            mappings: mappings
        };

        fetch('/api/mismatch/save', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        }).then(() => {
            alert('저장 완료');
            fetchFiles(); // 저장 후 목록 리로드
        }).catch(error => alert('저장 중 오류가 발생했습니다.'));
    });

    fileSelect.addEventListener('change', function () {
        selectedFile = this.value;
        autoSetDictionaryFileName(selectedFile);
        fetchData(selectedFile);
    });

    labelSelect.addEventListener('change', function () {
        selectedLabel = this.value;
        renderEntries();
    });

    fieldSelect.addEventListener('change', function () {
        renderEntries();
    });

    fetchFiles();
});
