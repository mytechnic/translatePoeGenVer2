// API에서 데이터 가져오기
function loadDictionary() {
    fetch('/api/dictionary')
        .then(response => {
            if (!response.ok) {
                throw new Error('Failed to fetch dictionary data');
            }
            return response.json();
        })
        .then(data => {
            const tableBody = document.getElementById('dictionary-table-body');
            tableBody.innerHTML = ''; // 기존 데이터 초기화
            data.data.forEach(entry => {
                const row = document.createElement('tr');

                const engCell = document.createElement('td');
                const engInput = document.createElement('input');
                engInput.type = 'text';
                engInput.value = entry.eng;
                engInput.className = 'editable';
                engCell.appendChild(engInput);

                const korCell = document.createElement('td');
                const korInput = document.createElement('input');
                korInput.type = 'text';
                korInput.value = entry.kor;
                korInput.className = 'editable';
                korCell.appendChild(korInput);

                const deleteCell = document.createElement('td');
                const deleteButton = document.createElement('button');
                deleteButton.textContent = '삭제';
                deleteButton.addEventListener('click', function () {
                    tableBody.removeChild(row); // 해당 행 삭제
                });
                deleteCell.appendChild(deleteButton);

                row.appendChild(engCell);
                row.appendChild(korCell);
                row.appendChild(deleteCell);
                tableBody.appendChild(row);
            });
        })
        .catch(error => {
            console.error('Error:', error);
        });
}

// 페이지 로드 시 사전 데이터 가져오기
loadDictionary();