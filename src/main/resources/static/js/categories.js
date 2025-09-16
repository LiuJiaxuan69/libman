document.addEventListener('DOMContentLoaded', () => {
  initCategoryPage();
});

async function initCategoryPage() {
    try {
      console.time('category.list.fetch');
      const resp = await fetch('/category/list');
      console.timeEnd('category.list.fetch');

      console.time('category.list.parse');
      const body = await resp.json();
      console.timeEnd('category.list.parse');

      const cats = body.data || [];
      console.time('category.render.chips');
      renderCategoryChips(cats);
      console.timeEnd('category.render.chips');
    } catch (e) {
      console.error('加载分类失败', e);
    }
}

function renderCategoryChips(categories) {
  const bar = document.getElementById('category-bar');
  bar.innerHTML = '';
  categories.forEach(c => {
    const chip = document.createElement('div');
    chip.className = 'category-chip';
    chip.dataset.id = c.id;
    chip.textContent = c.categoryName || c.category_name || c.name || ('#' + c.id);
    chip.addEventListener('click', async () => {
      chip.classList.toggle('selected');
      await loadBooksBySelectedCategories();
    });
    bar.appendChild(chip);
  });
}

async function loadBooksBySelectedCategories() {
  const selected = Array.from(document.querySelectorAll('.category-chip.selected')).map(d => parseInt(d.dataset.id, 10));
  const emptyDiv = document.getElementById('empty');
  const container = document.getElementById('book-container');
  container.innerHTML = '';
  if (selected.length === 0) {
    emptyDiv.style.display = 'block';
    return;
  }
  emptyDiv.style.display = 'none';

    try {
      console.time('books.byCategory.fetch');
      const resp = await fetch('/book/getBooksByCategoryIds', { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify(selected) });
      console.timeEnd('books.byCategory.fetch');

      console.time('books.byCategory.parse');
      const result = await resp.json();
      console.timeEnd('books.byCategory.parse');

      const books = result.data || [];
    if (!books.length) {
      container.innerHTML = '<div class="empty">该分类下暂无图书</div>';
      return;
    }
      // 使用 index 页面相同的渲染样式
      console.time('books.render.loop');
      books.forEach(book => {
      const card = document.createElement('div');
      card.className = 'book-card';
      card.dataset.bookId = book.id;
      let tagsStr = '';
      try { if (book.tags) { const arr = JSON.parse(book.tags); if (Array.isArray(arr)) tagsStr = arr.join(', '); else tagsStr = book.tags; } } catch(e) { tagsStr = book.tags; }
  card.innerHTML = `
        <div class="card-header">
          <div class="title">${escapeHtml(book.bookName)}</div>
          <div class="status">${escapeHtml(book.status === 1 ? '可借阅' : book.status === 2 ? '已借出' : '无效')}</div>
        </div>
        <div class="card-details">
          <p>书籍ID: ${book.id}</p>
          <p>分类: ${escapeHtml(book.categoryNames || '')}</p>
          <p>标签: ${escapeHtml(tagsStr)}</p>
          <p>作者: ${escapeHtml(book.author)}</p>
          <p>出版社: ${escapeHtml(book.publish)}</p>
          <p>定价: ${escapeHtml(book.price)}</p>
          <div class="card-actions">${getActionButton(book)}</div>
        </div>
      `;
      card.addEventListener('click', (e) => {
        if (!e.target.classList.contains('action-btn')) card.classList.toggle('expanded');
      });
      container.appendChild(card);
      });
      console.timeEnd('books.render.loop');
  } catch (err) {
    console.error('获取分类书籍失败', err);
    container.innerHTML = '<div class="empty">加载失败</div>';
  }
}

function escapeHtml(s) { return String(s || '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;').replace(/'/g,'&#039;'); }

const LOCAL_BORROW_KEY = 'borrowed_books';

function loadBorrowedSet() {
  try {
    const raw = localStorage.getItem(LOCAL_BORROW_KEY);
    if (!raw) return new Set();
    const arr = JSON.parse(raw);
    return new Set(arr.map(x => Number(x)));
  } catch (e) {
    return new Set();
  }
}

const borrowedLocalSet = loadBorrowedSet();

function saveBorrowedSet() {
  try {
    localStorage.setItem(LOCAL_BORROW_KEY, JSON.stringify(Array.from(borrowedLocalSet)));
  } catch (e) { }
}

function addBorrowedId(id) {
  borrowedLocalSet.add(Number(id));
  saveBorrowedSet();
}

function removeBorrowedId(id) {
  borrowedLocalSet.delete(Number(id));
  saveBorrowedSet();
}

function getActionButton(book) {
  const isMyBorrow = borrowedLocalSet.has(Number(book.id));
  if (isMyBorrow) {
    return `<button class="action-btn action-return" onclick="returnBook(${book.id})">归还</button>`;
  } else if (book.isBorrowed) {
    return `<button class="action-btn action-disabled" disabled>已借阅</button>`;
  } else if (book.status === 1) {
    return `<button class="action-btn action-borrow" onclick="borrowBook(${book.id})">借阅</button>`;
  } else {
    return `<button class="action-btn action-disabled" disabled>已借阅</button>`;
  }
}

function borrowBook(bookId) {
  fetch('/book/borrowBook', { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify(bookId) })
    .then(r => r.json()).then(res => {
      if (res.status === 'SUCCESS') {
        showToast('借阅成功');
        addBorrowedId(bookId);
        // update card UI
        const card = document.querySelector(`.book-card[data-book-id='${bookId}']`);
        if (card) {
          card.dataset.cooldown = 3;
          const actions = card.querySelector('.card-actions');
          if (actions) actions.innerHTML = `<button class="action-btn action-disabled" disabled>3</button>`;
          startCooldown && startCooldown(bookId, 3);
        }
      } else {
        showToast('借阅失败:'+res.errorMessage);
      }
    }).catch(e=>{ showToast('借阅网络错误'); });
}

function returnBook(bookId) {
  fetch('/book/returnBook', { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify(bookId) })
    .then(r => r.json()).then(res => {
      if (res.status === 'SUCCESS') {
        showToast('归还成功');
        removeBorrowedId(bookId);
        const card = document.querySelector(`.book-card[data-book-id='${bookId}']`);
        if (card) {
          card.dataset.cooldown = 3;
          const actions = card.querySelector('.card-actions');
          if (actions) actions.innerHTML = `<button class="action-btn action-disabled" disabled>3</button>`;
          startCooldown && startCooldown(bookId, 3);
        }
      } else {
        showToast('归还失败:'+res.errorMessage);
      }
    }).catch(e=>{ showToast('归还网络错误'); });
}

function showToast(message, duration = 2000) {
  const container = document.getElementById('toast-container');
  const toast = document.createElement('div');
  toast.className = 'toast';
  toast.textContent = message;
  container.appendChild(toast);
  setTimeout(()=>toast.classList.add('show'),10);
  setTimeout(()=>{ toast.classList.remove('show'); setTimeout(()=>container.removeChild(toast),300); }, duration);
}
