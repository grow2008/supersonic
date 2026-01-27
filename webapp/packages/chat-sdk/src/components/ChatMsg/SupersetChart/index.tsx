import { useEffect, useMemo, useState } from 'react';
import { Button, Dropdown, message } from 'antd';
import {
  MsgDataType,
  SupersetChartResponseType,
  SupersetDashboardType,
} from '../../../common/type';
import { fetchSupersetDashboards, pushSupersetChartToDashboard } from '../../../service';
import { isMobile } from '../../../utils/utils';

type Props = {
  id: string | number;
  data: MsgDataType;
};

const DEFAULT_HEIGHT = 800;

const SupersetChart: React.FC<Props> = ({ id, data }) => {
  const [embedUrl, setEmbedUrl] = useState('');
  const [height, setHeight] = useState(DEFAULT_HEIGHT);
  const [dashboards, setDashboards] = useState<SupersetDashboardType[]>([]);
  const [dashboardsLoading, setDashboardsLoading] = useState(false);
  const [pushLoading, setPushLoading] = useState(false);
  const response = data.response as SupersetChartResponseType;
  const webPage = response?.webPage;
  const guestToken = response?.guestToken;

  const buildParamValue = (value: any) => {
    if (value === null || value === undefined) {
      return '';
    }
    if (typeof value === 'string') {
      return value;
    }
    return JSON.stringify(value);
  };

  const params = useMemo(() => {
    const rawParams = webPage?.params || webPage?.paramOptions || [];
    return Array.isArray(rawParams) ? rawParams : [];
  }, [webPage]);

  useEffect(() => {
    const heightValue =
      params?.find((option: any) => option.paramType === 'FORWARD' && option.key === 'height')
        ?.value || DEFAULT_HEIGHT;
    setHeight(heightValue);
    let urlValue = webPage?.url || '';
    if (!urlValue) {
      return;
    }
    const valueParams = (params || [])
      .filter((option: any) => option.paramType !== 'FORWARD')
      .reduce((result: any, item: any) => {
        result[item.key] = item.value;
        return result;
      }, {});
    if (guestToken && !valueParams.guestToken) {
      valueParams.guestToken = guestToken;
    }
    const keys = Object.keys(valueParams || {});
    if (keys.length > 0) {
      const queryString = keys
        .map(key => `${key}=${encodeURIComponent(buildParamValue(valueParams[key]))}`)
        .join('&');
      if (urlValue.includes('?')) {
        urlValue = urlValue.replace('?', `?${queryString}&`);
      } else {
        urlValue = `${urlValue}?${queryString}`;
      }
    }
    setEmbedUrl(urlValue);
  }, [params, webPage?.url, guestToken]);

  useEffect(() => {
    if (Array.isArray(response?.dashboards)) {
      setDashboards(response?.dashboards || []);
    }
  }, [response?.dashboards]);

  useEffect(() => {
    const shouldFetch = response?.dashboards === undefined && response?.pluginId;
    if (!shouldFetch) {
      return;
    }
    setDashboardsLoading(true);
    fetchSupersetDashboards(response.pluginId)
      .then(res => {
        setDashboards(res?.data || []);
      })
      .catch(() => {
        message.error('获取 Dashboard 列表失败');
      })
      .finally(() => {
        setDashboardsLoading(false);
      });
  }, [response?.dashboards, response?.pluginId]);

  const handlePush = (dashboardId?: number) => {
    if (!dashboardId) {
      return;
    }
    if (!response?.chartId) {
      message.error('Chart 信息缺失');
      return;
    }
    setPushLoading(true);
    pushSupersetChartToDashboard({
      pluginId: response?.pluginId,
      dashboardId,
      chartId: response.chartId,
    })
      .then(() => {
        message.success('已推送到 Dashboard');
      })
      .catch(() => {
        message.error('推送失败');
      })
      .finally(() => {
        setPushLoading(false);
      });
  };

  const menuItems =
    dashboards.length > 0
      ? dashboards.map(item => ({
          key: String(item.id ?? item.title ?? 'unknown'),
          label: item.title || `Dashboard ${item.id}`,
        }))
      : [
          {
            key: 'empty',
            label: '暂无 Dashboard',
            disabled: true,
          },
        ];

  const showPushButton =
    !response?.fallback && response?.pluginId && response?.chartId && response?.webPage?.url;

  return (
    <>
      {showPushButton && (
        <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 8 }}>
          <Dropdown
            menu={{
              items: menuItems,
              onClick: info => {
                const dashboardId = Number(info.key);
                handlePush(Number.isNaN(dashboardId) ? undefined : dashboardId);
              },
            }}
            disabled={dashboardsLoading}
          >
            <Button size="small" loading={dashboardsLoading || pushLoading}>
              推送到 Dashboard
            </Button>
          </Dropdown>
        </div>
      )}
      <iframe
        id={`supersetIframe_${id}`}
        name={`supersetIframe_${id}`}
        src={embedUrl}
        style={{
          width: isMobile ? 'calc(100vw - 20px)' : 'calc(100vw - 410px)',
          height,
          border: 'none',
        }}
        title="supersetIframe"
        allowFullScreen
      />
    </>
  );
};

export default SupersetChart;
