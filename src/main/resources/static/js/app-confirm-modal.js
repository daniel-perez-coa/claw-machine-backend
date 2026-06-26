window.showAppConfirmModal = (() => {
    let modalElement = null;
    let modalInstance = null;
    let resolver = null;

    function ensureModal() {
        if (modalElement) {
            return;
        }

        modalElement = document.createElement('div');
        modalElement.className = 'modal fade';
        modalElement.id = 'appConfirmModal';
        modalElement.tabIndex = -1;
        modalElement.setAttribute('aria-hidden', 'true');
        modalElement.innerHTML = `
            <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content app-confirm-modal">
                    <div class="modal-body app-confirm-modal__body">
                        <h2 class="app-confirm-modal__title"></h2>
                        <p class="app-confirm-modal__text"></p>
                        <div class="app-confirm-modal__actions">
                            <button type="button" class="app-confirm-modal__button app-confirm-modal__button--cancel" data-app-confirm-action="cancel">
                                Cancelar
                            </button>
                            <button type="button" class="app-confirm-modal__button app-confirm-modal__button--confirm" data-app-confirm-action="confirm">
                                Confirmar
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        `;

        document.body.appendChild(modalElement);

        const titleElement = modalElement.querySelector('.app-confirm-modal__title');
        const textElement = modalElement.querySelector('.app-confirm-modal__text');
        const cancelButton = modalElement.querySelector('[data-app-confirm-action="cancel"]');
        const confirmButton = modalElement.querySelector('[data-app-confirm-action="confirm"]');

        cancelButton.addEventListener('click', () => {
            if (modalElement._appConfirm.preventDismiss) {
                return;
            }

            resolver?.(false);
            resolver = null;
            modalInstance?.hide();
        });

        confirmButton.addEventListener('click', () => {
            resolver?.(true);
            resolver = null;
            modalInstance?.hide();
        });

        modalElement.addEventListener('hidden.bs.modal', () => {
            if (resolver) {
                resolver(false);
                resolver = null;
            }
        });

        modalElement.addEventListener('hide.bs.modal', (event) => {
            if (modalElement._appConfirm.preventDismiss && resolver) {
                event.preventDefault();
            }
        });

        modalInstance = new window.bootstrap.Modal(modalElement);

        modalElement._appConfirm = {
            titleElement,
            textElement,
            cancelButton,
            confirmButton
        };
    }

    return function showAppConfirmModal(options = {}) {
        ensureModal();

        const {
            title = 'Confirmar accion',
            body = '',
            bodyHtml = '',
            confirmText = 'Confirmar',
            cancelText = 'Cancelar',
            cancelVariant = 'cancel',
            confirmVariant = 'success',
            hideCancel = false,
            preventDismiss = false
        } = options;

        const { titleElement, textElement, cancelButton, confirmButton } = modalElement._appConfirm;
        modalElement._appConfirm.preventDismiss = preventDismiss;

        titleElement.textContent = title;
        if (bodyHtml) {
            textElement.innerHTML = bodyHtml;
        } else {
            textElement.textContent = body;
        }
        cancelButton.textContent = cancelText;
        cancelButton.className = `app-confirm-modal__button app-confirm-modal__button--${cancelVariant}`;
        cancelButton.hidden = hideCancel;
        confirmButton.textContent = confirmText;
        confirmButton.className = `app-confirm-modal__button app-confirm-modal__button--confirm app-confirm-modal__button--${confirmVariant}`;

        modalInstance.show();

        return new Promise((resolve) => {
            resolver = resolve;
        });
    };
})();
