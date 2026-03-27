document.addEventListener('DOMContentLoaded', () => {
    const form = htmx.find('#feedback-form');
    if (!form) return;

    const channelId = form.dataset.channelId;

    const showError = (message) => {
        const errorEl = htmx.find('#error-message');
        errorEl.textContent = message;
    };

    const resetButton = () => {
        const btn = htmx.find('#submit-btn');
        btn.disabled = false;
        btn.textContent = 'Submit Feedback';
    };

    form.addEventListener('submit', async (e) => {
        e.preventDefault();

        const btn = htmx.find('#submit-btn');
        const errorEl = htmx.find('#error-message');
        btn.disabled = true;
        btn.textContent = 'Submitting...';
        errorEl.textContent = '';

        const ratings = [...htmx.findAll(form, 'fieldset.rating-group')]
            .map((group) => htmx.find(group, 'input[type="radio"]:checked'))
            .filter(Boolean)
            .map((checked) => ({
                id: parseInt(checked.name.replace('rating-', ''), 10),
                score: parseInt(checked.value, 10),
            }));

        const comment = htmx.find('#detailed-comment').value || null;

        const payload = {
            ratings,
            detailedComment: comment,
        };

        try {
            const response = await fetch(`/v1/feedback/channel/${channelId}/submit-feedback`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload),
            });

            if (response.ok) {
                htmx.ajax('GET', `/session/${channelId}/thank-you`, {
                    target: '#feedback-form',
                    swap: 'outerHTML',
                });
            } else {
                resetButton();
                showError('Something went wrong. Please try again.');
            }
        } catch {
            resetButton();
            showError('Network error. Please try again.');
        }
    });
});
