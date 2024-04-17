import { GithubOutlined } from '@ant-design/icons';
import { DefaultFooter } from '@ant-design/pro-components';
import React from 'react';

const Footer: React.FC = () => {
  return (
    <DefaultFooter
      style={{
        background: 'none',
      }}
      links={[
        {
          key: 'github',
          title: <><GithubOutlined /> 多龙 GitHub</>,
          href: 'https://github.com/Miaowzhz',
          blankTarget: true,
        },
      ]}
    />
  );
};

export default Footer;
