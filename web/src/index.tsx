import React from 'react';
import {createRoot} from 'react-dom/client';
import AccountApp from './AccountApp';
import {watchViewport} from './lib/viewport';
watchViewport();
createRoot(document.getElementById('root')!).render(<AccountApp/>);
