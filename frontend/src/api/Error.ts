export type ErrorResponse = {
  error: boolean,
  status: string,
  message: string;
};

export type AxiosError = {
  response?: {
    status: string,
    data: any,
  },
};

export const errorHandler = (err: AxiosError): ErrorResponse => {
  if (!err.response) {
    return {
      error: true,
      status: 'XXX', // Request didn't initiate; no status returned
      message: 'An error occurred initiating the REST request',
    };
  }

  return {
    error: true,
    status: err.response.status,
    message: err.response.data.message || 'An error occurred; please try again later.',
  };
};

export const isError = (res: any): boolean => res && res.error;
