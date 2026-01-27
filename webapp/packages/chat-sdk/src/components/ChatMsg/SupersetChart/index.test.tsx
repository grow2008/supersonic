import { render, waitFor } from '@testing-library/react';
import SupersetChart from './index';

const buildData = (response: any) =>
  ({
    response,
    queryMode: 'SUPERSET',
    queryState: 'SUCCESS',
    queryColumns: [],
    queryResults: [],
  } as any);

describe('SupersetChart', () => {
  test('injects guestToken into iframe url', async () => {
    const data = buildData({
      webPage: { url: 'https://superset.example.com/embed', params: [] },
      guestToken: 'token-123',
    });
    const { getByTitle } = render(<SupersetChart id={1} data={data} />);
    await waitFor(() => {
      expect(getByTitle('supersetIframe')).toHaveAttribute(
        'src',
        'https://superset.example.com/embed?guestToken=token-123'
      );
    });
  });

  test('keeps url when no extra params', async () => {
    const data = buildData({
      webPage: { url: 'https://superset.example.com/embed', params: [] },
    });
    const { getByTitle } = render(<SupersetChart id={1} data={data} />);
    await waitFor(() => {
      expect(getByTitle('supersetIframe')).toHaveAttribute(
        'src',
        'https://superset.example.com/embed'
      );
    });
  });

  test('renders empty src when url missing', async () => {
    const data = buildData({
      webPage: { url: '', params: [] },
    });
    const { getByTitle } = render(<SupersetChart id={1} data={data} />);
    await waitFor(() => {
      expect(getByTitle('supersetIframe')).toHaveAttribute('src', '');
    });
  });

  test('shows push button when dashboards exist', () => {
    const data = buildData({
      webPage: { url: 'https://superset.example.com/embed', params: [] },
      pluginId: 1,
      chartId: 2,
      dashboards: [{ id: 10, title: 'Sales' }],
    });
    const { getByText } = render(<SupersetChart id={1} data={data} />);
    expect(getByText('推送到 Dashboard')).toBeTruthy();
  });
});
