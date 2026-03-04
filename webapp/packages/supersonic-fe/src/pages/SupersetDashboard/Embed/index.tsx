import { useLocation } from '@umijs/max';
import { embedDashboard } from '@superset-ui/embedded-sdk';
import { message } from 'antd';
import queryString from 'query-string';
import React, { useEffect, useMemo, useRef } from 'react';
import { fetchSupersetGuestToken } from '../service';

const normalizeDomain = (domain?: string) => {
  if (!domain) {
    return undefined;
  }
  return domain.endsWith('/') ? domain.slice(0, -1) : domain;
};

const SupersetDashboardEmbed: React.FC = () => {
  const location = useLocation();
  const query = useMemo(() => queryString.parse(location.search) || {}, [location.search]);
  const embeddedId = typeof query.embeddedId === 'string' ? query.embeddedId : '';
  const supersetDomain = normalizeDomain(
    typeof query.supersetDomain === 'string' ? query.supersetDomain : undefined
  );
  const pluginId = query.pluginId ? Number(query.pluginId) : undefined;
  const title = typeof query.title === 'string' ? query.title : 'Superset Dashboard';
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!embeddedId || !supersetDomain || !containerRef.current) {
      return;
    }
    let cancelled = false;
    const fetchGuestToken = async () => {
      const resp = await fetchSupersetGuestToken({ pluginId, embeddedId });
      const token = resp?.token;
      if (!token) {
        throw new Error('guest token missing');
      }
      return token;
    };
    embedDashboard({
      id: embeddedId,
      supersetDomain,
      mountPoint: containerRef.current,
      iframeTitle: title,
      fetchGuestToken,
      dashboardUiConfig: {
        hideTitle: false,
        hideTab: true,
        hideChartControls: false,
        filters: { visible: false, expanded: false },
      },
    }).catch(() => {
      if (!cancelled) {
        message.error('Superset 嵌入失败');
      }
    });
    return () => {
      cancelled = true;
      if (containerRef.current) {
        containerRef.current.replaceChildren();
      }
    };
  }, [embeddedId, supersetDomain, pluginId, title]);

  if (!embeddedId || !supersetDomain) {
    return <div style={{ padding: 24 }}>嵌入信息缺失，无法渲染看板。</div>;
  }

  return (
    <div style={{ width: '100vw', height: '100vh', background: '#fff' }}>
      <div ref={containerRef} style={{ width: '100%', height: '100%' }} />
    </div>
  );
};

export default SupersetDashboardEmbed;
