import LegalPage from '../components/LegalPage'

const SECTIONS = [
  { titleKey: 'common.terms_section1_title', textKey: 'common.terms_section1_text' },
  { titleKey: 'common.terms_section2_title', textKey: 'common.terms_section2_text' },
  { titleKey: 'common.terms_section3_title', textKey: 'common.terms_section3_text' },
  { titleKey: 'common.terms_section4_title', textKey: 'common.terms_section4_text' },
]

export default function TermsPage() {
  return <LegalPage titleKey="common.terms_title" sections={SECTIONS} updatedKey="common.terms_updated" />
}
