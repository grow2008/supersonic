export type PluginConfigType = {
  url?: string;
  params?: any;
  paramOptions?: any;
  valueParams?: any;
  forwardParam?: any;
  enabled?: boolean;
  baseUrl?: string;
  accessToken?: string;
  timeoutSeconds?: number;
  datasourceType?: string;
  vizType?: string;
  vizTypeLlmEnabled?: boolean;
  vizTypeLlmTopN?: number;
  vizTypeAllowList?: string[];
  vizTypeDenyList?: string[];
  vizTypeLlmChatModelId?: number;
  vizTypeLlmPrompt?: string;
  formData?: any;
  height?: number;
};

export enum PluginTypeEnum {
  WEB_PAGE = 'WEB_PAGE',
  WEB_SERVICE = 'WEB_SERVICE',
  AGENT_SERVICE = 'AGENT_SERVICE',
  SUPERSET = 'SUPERSET',
  NL2SQL_LLM = 'NL2SQL_LLM',
}

export enum ParseModeEnum {
  EMBEDDING_RECALL = 'EMBEDDING_RECALL',
  FUNCTION_CALL = 'FUNCTION_CALL',
}

export enum ParamTypeEnum {
  CUSTOM = 'CUSTOM',
  SEMANTIC = 'SEMANTIC',
  FORWARD = 'FORWARD',
}

export type PluginType = {
  id: number;
  type: PluginTypeEnum;
  dataSetList: number[];
  modelList: number[];
  pattern: string;
  parseMode: ParseModeEnum;
  parseModeConfig: string;
  name: string;
  config: PluginConfigType;
};

export type ModelType = {
  id: number | string;
  parentId: number;
  name: string;
  bizName: string;
};

export type DimensionType = {
  id: number;
  name: string;
  bizName: string;
};

export type FunctionParamType = {
  type: string;
  properties: Record<string, { type: string; description: string }>;
  required: string[];
};

export type FunctionType = {
  name: string;
  description: string;
  parameters: FunctionParamType;
  examples: string[];
};

export type FunctionParamFormItemType = {
  id: string;
  name?: string;
  type?: string;
  description?: string;
};
