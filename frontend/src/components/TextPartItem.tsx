import React from 'react';
import ReactMarkdown from 'react-markdown';
import remarkMath from 'remark-math';
import remarkGfm from 'remark-gfm';
import { TextPart } from '../store/chatStore';

interface TextPartItemProps {
    part: TextPart;
}

const TextPartItem: React.FC<TextPartItemProps> = ({ part }) => {
    return (
        <ReactMarkdown
            remarkPlugins={[remarkMath, remarkGfm]}
        >
            {part.content}
        </ReactMarkdown>
    );
};

export default TextPartItem; 