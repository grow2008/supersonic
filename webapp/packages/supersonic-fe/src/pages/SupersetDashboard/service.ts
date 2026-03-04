import request from 'umi-request';

export type SupersetDashboardManageResp = {
  pluginId?: number;
  supersetDomain?: string;
  dashboards?: SupersetDashboardItem[];
};

export type SupersetDashboardItem = {
  id: number;
  title?: string;
  embeddedId?: string;
  supersetDomain?: string;
  editUrl?: string;
  tags?: string[];
};

export type SupersetGuestTokenResp = {
  token?: string;
};

export function fetchSupersetManualDashboards(pluginId?: number): Promise<SupersetDashboardManageResp> {
  return request.post(`${process.env.CHAT_API_BASE_URL}superset/dashboards/manage`, {
    data: { pluginId },
  });
}

export function createSupersetDashboard(params: {
  pluginId?: number;
  title: string;
}): Promise<SupersetDashboardItem> {
  return request.post(`${process.env.CHAT_API_BASE_URL}superset/dashboard/create`, {
    data: params,
  });
}

export function deleteSupersetDashboard(params: {
  pluginId?: number;
  dashboardId: number;
}): Promise<boolean> {
  return request.post(`${process.env.CHAT_API_BASE_URL}superset/dashboard/delete`, {
    data: params,
  });
}

export function fetchSupersetGuestToken(params: {
  pluginId?: number;
  embeddedId: string;
}): Promise<SupersetGuestTokenResp> {
  return request.post(`${process.env.CHAT_API_BASE_URL}superset/guest-token`, {
    data: params,
  });
}
