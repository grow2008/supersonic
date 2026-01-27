import { render } from '@testing-library/react';
import ExecuteItem from './ExecuteItem';
import { MsgDataType } from '../../common/type';

jest.mock('../ChatMsg', () => () => 'chat-msg');
jest.mock('react-syntax-highlighter', () => ({
  Prism: () => null,
}));
jest.mock('react-syntax-highlighter/dist/esm/styles/prism', () => ({
  solarizedlight: {},
}));

const buildSupersetData = (fallback = false): MsgDataType =>
  ({
    queryMode: 'SUPERSET',
    queryColumns: [],
    queryResults: [],
    response: {
      name: 'Superset',
      pluginId: 1,
      pluginType: 'SUPERSET',
      webPage: {
        url: 'https://superset.example.com/embed',
        params: [],
        paramOptions: [],
        valueParams: [],
      },
      dashboards: [],
      fallback,
    },
  }) as MsgDataType;

describe('ExecuteItem', () => {
  it('renders Superset chart when queryMode is SUPERSET', () => {
    const data = buildSupersetData(false);
    const { getByTitle } = render(
      <ExecuteItem
        queryId={1}
        question="test"
        queryMode="SUPERSET"
        executeLoading={false}
        chartIndex={0}
        data={data}
      />
    );

    expect(getByTitle('supersetIframe')).toBeInTheDocument();
  });

  it('falls back to default chart when Superset response is fallback', () => {
    const data = buildSupersetData(true);
    const { queryByTitle, getByText } = render(
      <ExecuteItem
        queryId={1}
        question="test"
        queryMode="SUPERSET"
        executeLoading={false}
        chartIndex={0}
        data={data}
      />
    );

    expect(queryByTitle('supersetIframe')).toBeNull();
    expect(getByText('chat-msg')).toBeInTheDocument();
  });
});
