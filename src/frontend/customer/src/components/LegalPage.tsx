import { Typography, Divider } from 'antd'
import { Link } from 'react-router-dom'
import { useTranslation } from '../i18n'
import AuthCard from './AuthCard'
import LanguageSwitcher from './LanguageSwitcher'

const { Title, Paragraph } = Typography

interface LegalPageProps {
  titleKey: string
  sections: { titleKey: string; textKey: string }[]
  updatedKey: string
}

export default function LegalPage({ titleKey, sections, updatedKey }: LegalPageProps) {
  const { t } = useTranslation()

  return (
    <AuthCard title={t(titleKey)}>
      <LanguageSwitcher />
      <Typography style={{ padding: '8px 0' }}>
        {sections.map((s, i) => (
          <div key={i}>
            <Title level={4}>{i + 1}. {t(s.titleKey)}</Title>
            <Paragraph>{t(s.textKey)}</Paragraph>
          </div>
        ))}
        <Divider />
        <Paragraph type="secondary" style={{ textAlign: 'center' }}>
          {t(updatedKey)}
        </Paragraph>
      </Typography>
      <div className="auth-footer-link">
        <Link to="/register">{t('common.back')}</Link>
      </div>
    </AuthCard>
  )
}
