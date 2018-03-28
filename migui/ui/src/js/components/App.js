import React from 'react';
//import Header from './Header';
//import Footer from './Footer';

import darkBaseTheme from 'material-ui/styles/baseThemes/darkBaseTheme';
import MuiThemeProvider from 'material-ui/styles/MuiThemeProvider';
import getMuiTheme from 'material-ui/styles/getMuiTheme';


class App extends React.Component {
    constructor(props) {
        super(props);
    }


    render() {
        return (
            <MuiThemeProvider muiTheme={getMuiTheme(darkBaseTheme)}>
                <div>
                    <Header/>

                    <div style={{
                        position: 'absolute',
                        // top: '60px',
                        top: '18px',
                        left: '80px',
                        zIndex: '1'
                    }}>
                    </div>

                    <div style={{
                        position: 'absolute',
                        // top: '43px',
                        top: '60px',
                        bottom: '25px',
                        right: '0px',
                        width: '400px',
                        background: '#1d1d1d',
                        borderTop: '2px solid #3d3d3d',
                        borderLeft: '1px solid #3d3d3d'
                    }}>
                    </div>

                    <Footer/>
                </div>
            </MuiThemeProvider>
        );
    }
}

App.displayName = 'App;';