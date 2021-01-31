import React, { useEffect, useState } from 'react';
import MonacoEditor from 'react-monaco-editor';
import styled from 'styled-components';
import Language, { fromString } from '../../api/Language';
import { DefaultCodeType } from '../../api/Problem';

type EditorProps = {
  onLanguageChange: (language: string) => void,
  codeMap: DefaultCodeType | null,
};

const Content = styled.div`
  height: 100%;
`;

// This function refreshes the width of Monaco editor upon change in container size
function ResizableMonacoEditor(props: EditorProps) {
  const [currentLanguage, setCurrentLanguage] = useState<Language>(Language.Python);
  const [codeEditor, setCodeEditor] = useState<any>(null);

  const { onLanguageChange, codeMap } = props;

  const handleEditorDidMount = (editor: any) => {
    setCodeEditor(editor);
    window.addEventListener('resize', () => {
      editor.layout();
    });
    window.addEventListener('secondaryPanelSizeChange', () => {
      editor.layout();
    });
  };

  const handleLanguageChange = (language: Language) => {
    // Save the code for this language
    // eslint-disable-next-line no-unused-expressions
    if (codeMap != null) {
      codeMap[currentLanguage] = codeEditor.getValue();
      codeEditor!.setValue(codeMap[language]);
    }

    // Change the language and initial code for the editor
    setCurrentLanguage(language);
    onLanguageChange(language);
  };

  useEffect(() => {
    if (codeMap != null && codeEditor != null) {
      codeEditor.setValue(codeMap[currentLanguage]);
    }
  }, [currentLanguage, codeMap, setCodeEditor]);

  return (
    <Content>
      <select
        onChange={(e) => handleLanguageChange(fromString(e.target.value))}
        value={fromString(currentLanguage)}
      >
        {
          Object.keys(Language).map((language) => (
            <option key={language} value={language}>{language}</option>
          ))
        }
      </select>
      <MonacoEditor
        height="100%"
        editorDidMount={handleEditorDidMount}
        language={currentLanguage}
        defaultValue={codeMap ? codeMap[currentLanguage] : 'Loading...'}
      />
    </Content>
  );
}

export default ResizableMonacoEditor;
