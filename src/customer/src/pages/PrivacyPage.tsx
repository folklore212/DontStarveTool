import LegalPage from '../components/LegalPage'

const SECTIONS = [
  { titleKey: 'common.privacy_section1_title', textKey: 'common.privacy_section1_text' },
  { titleKey: 'common.privacy_section2_title', textKey: 'common.privacy_section2_text' },
  { titleKey: 'common.privacy_section3_title', textKey: 'common.privacy_section3_text' },
  { titleKey: 'common.privacy_section4_title', textKey: 'common.privacy_section4_text' },
]

export default function PrivacyPage() {
  return <LegalPage titleKey="common.privacy_title" sections={SECTIONS} updatedKey="common.privacy_updated" />
}
