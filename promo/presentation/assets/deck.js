(function () {
    var root = document.documentElement;
    var slides = Array.from(document.querySelectorAll('.slide'));
    var totalSlides = slides.length;
    var currentSlide = 1;
    var dotsContainer = document.getElementById('dots');
    var counter = document.getElementById('counter');

    function applyTheme(theme) {
        root.setAttribute('data-theme', theme);
        localStorage.setItem('Frap-theme', theme);
    }

    function updateSlides() {
        slides.forEach(function (slide) {
            var num = parseInt(slide.getAttribute('data-slide'), 10);
            slide.classList.remove('active', 'prev');
            if (num === currentSlide) {
                slide.classList.add('active');
            } else if (num < currentSlide) {
                slide.classList.add('prev');
            }
        });

        Array.from(document.querySelectorAll('.dot')).forEach(function (dot, idx) {
            dot.classList.toggle('active', idx + 1 === currentSlide);
        });

        counter.textContent = currentSlide + ' / ' + totalSlides;
    }

    function goToSlide(n) {
        if (n < 1 || n > totalSlides) return;
        currentSlide = n;
        updateSlides();
    }

    function nextSlide() {
        if (currentSlide < totalSlides) {
            currentSlide += 1;
            updateSlides();
        }
    }

    function prevSlide() {
        if (currentSlide > 1) {
            currentSlide -= 1;
            updateSlides();
        }
    }

    for (var i = 1; i <= totalSlides; i += 1) {
        var dot = document.createElement('div');
        dot.className = 'dot' + (i === 1 ? ' active' : '');
        (function (n) {
            dot.addEventListener('click', function () {
                goToSlide(n);
            });
        })(i);
        dotsContainer.appendChild(dot);
    }

    document.getElementById('nextBtn').addEventListener('click', nextSlide);
    document.getElementById('prevBtn').addEventListener('click', prevSlide);
    document.getElementById('themeBtn').addEventListener('click', function () {
        applyTheme(root.getAttribute('data-theme') === 'dark' ? 'light' : 'dark');
    });

    document.addEventListener('keydown', function (e) {
        if (e.key === 'ArrowRight' || e.key === ' ') {
            e.preventDefault();
            nextSlide();
        } else if (e.key === 'ArrowLeft') {
            e.preventDefault();
            prevSlide();
        } else if (e.key === 'Home') {
            e.preventDefault();
            goToSlide(1);
        } else if (e.key === 'End') {
            e.preventDefault();
            goToSlide(totalSlides);
        }
    });

    var touchStartX = 0;
    document.addEventListener('touchstart', function (e) {
        touchStartX = e.touches[0].clientX;
    });
    document.addEventListener('touchend', function (e) {
        var diff = touchStartX - e.changedTouches[0].clientX;
        if (Math.abs(diff) > 50) {
            if (diff > 0) nextSlide();
            else prevSlide();
        }
    });

    updateSlides();
})();
