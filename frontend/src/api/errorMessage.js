const FALLBACK_MESSAGE = 'Something went wrong. Please try again.';

const STATUS_MESSAGES = {
  401: 'Invalid credentials or your session has expired.',
  403: 'You do not have permission to perform this action.',
  404: 'The requested resource was not found.',
  409: 'This request conflicts with existing data.',
};

// Backend services (auth-service and the Gateway) share the same
// ErrorResponse/GatewayErrorResponse JSON shape, so one extractor covers both.
export function extractErrorMessage(error, fallback = FALLBACK_MESSAGE) {
  if (!error) {
    return fallback;
  }

  if (error.response) {
    const data = error.response.data;

    if (data && Array.isArray(data.fieldErrors) && data.fieldErrors.length > 0) {
      return data.fieldErrors.map((fieldError) => fieldError.message).join(' ');
    }

    if (data && typeof data.message === 'string' && data.message.trim() !== '') {
      return data.message;
    }

    return STATUS_MESSAGES[error.response.status] || fallback;
  }

  if (error.request) {
    return 'Unable to reach the server. Please check your connection and try again.';
  }

  return fallback;
}
