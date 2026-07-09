export type FilePreviewKind = 'image' | 'pdf' | 'text' | 'markdown' | 'html' | 'unsupported'

const imageExtensions = ['png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp', 'svg']
const markdownExtensions = ['md', 'markdown']
const htmlExtensions = ['html', 'htm']
const textExtensions = [
  'txt', 'log', 'csv', 'json', 'xml', 'sql',
  'java', 'vue', 'ts', 'tsx', 'js', 'jsx',
  'css', 'yaml', 'yml'
]
const officeExtensions = [
  'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx',
  'wps', 'et', 'dps', 'odt', 'ods', 'odp', 'rtf'
]

export function previewKindForFile(fileName?: string, contentType?: string): FilePreviewKind {
  const type = (contentType || '').toLowerCase()
  const ext = fileExtension(fileName)
  if (isOfficeLikeDocument(type, ext)) {
    return 'unsupported'
  }
  if (type.startsWith('image/') || imageExtensions.includes(ext)) {
    return 'image'
  }
  if (type === 'application/pdf' || ext === 'pdf') {
    return 'pdf'
  }
  if (markdownExtensions.includes(ext)) {
    return 'markdown'
  }
  if (type === 'text/html' || htmlExtensions.includes(ext)) {
    return 'html'
  }
  if (
    type.startsWith('text/')
    || type.includes('json')
    || type.includes('xml')
    || type.includes('javascript')
    || textExtensions.includes(ext)
  ) {
    return 'text'
  }
  return 'unsupported'
}

function fileExtension(fileName?: string) {
  const name = (fileName || '').toLowerCase()
  const dot = name.lastIndexOf('.')
  return dot >= 0 ? name.slice(dot + 1) : ''
}

function isOfficeLikeDocument(type: string, ext: string) {
  return officeExtensions.includes(ext)
    || type.includes('officedocument')
    || type.includes('msword')
    || type.includes('vnd.ms-')
    || type.includes('vnd.oasis.opendocument')
    || type === 'application/rtf'
}
