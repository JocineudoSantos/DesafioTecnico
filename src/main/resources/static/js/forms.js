(function () {
    function initConfirmations() {
        window.AppDom.bySelector("form[data-confirm]").forEach(function (form) {
            form.addEventListener("submit", function (event) {
                if (!window.confirm(form.getAttribute("data-confirm"))) {
                    event.preventDefault();
                }
            });
        });
    }

    function initPasswordToggle() {
        window.AppDom.bySelector("[data-password-toggle]").forEach(function (button) {
            button.addEventListener("click", function () {
                var container = button.closest(".password-field");
                if (!container) {
                    return;
                }

                var input = container.querySelector("[data-password-input]");
                if (!input) {
                    return;
                }

                var visible = input.type === "text";
                input.type = visible ? "password" : "text";
                button.textContent = visible ? "Mostrar" : "Ocultar";
            });
        });
    }

    function initCharacterCount() {
        window.AppDom.bySelector("[data-character-count]").forEach(function (field) {
            var output = field.parentElement.querySelector("[data-character-output]");
            if (!output) {
                return;
            }

            var sync = function () {
                var total = field.value.length;
                var max = field.getAttribute("maxlength");
                if (max) {
                    output.textContent = total + "/" + max + " caracteres";
                    return;
                }

                output.textContent = total + (total === 1 ? " caractere" : " caracteres");
            };

            field.addEventListener("input", sync);
            sync();
        });
    }

    function initAutoSubmit() {
        window.AppDom.bySelector("[data-auto-submit]").forEach(function (field) {
            field.addEventListener("change", function () {
                var form = field.form;
                if (form) {
                    form.submit();
                }
            });
        });
    }

    function initReservationDenialForms() {
        window.AppDom.bySelector("[data-reservation-denial-toggle]").forEach(function (button) {
            var formId = button.getAttribute("aria-controls");
            var form = formId ? document.getElementById(formId) : null;
            if (!form) {
                return;
            }

            button.addEventListener("click", function () {
                var isOpen = button.getAttribute("aria-expanded") === "true";
                form.hidden = isOpen;
                button.setAttribute("aria-expanded", String(!isOpen));
                button.textContent = isOpen ? "Negar" : "Fechar";
                if (!isOpen) {
                    var reason = form.querySelector("textarea[name='motivo']");
                    if (reason) {
                        reason.focus();
                    }
                }
            });
        });
    }
    window.AppDom.onReady(function () {
        initConfirmations();
        initPasswordToggle();
        initCharacterCount();
        initAutoSubmit();
        initReservationDenialForms();
    });
})();
