import MenuIcon from '@mui/icons-material/Menu';
import { AppBar, Toolbar, Typography } from '@mui/material';
import IconButton from '@mui/material/IconButton';
import React from 'react';
import { Link as RouterLink } from 'react-router-dom';

const Header = () => {
    return (
        <AppBar position="static">
            <Toolbar>
                <IconButton
                    size="large"
                    edge="start"
                    color="inherit"
                    aria-label="menu"
                    sx={{ mr: 2 }}
                >
                    <MenuIcon />
                </IconButton>
                <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
                    <RouterLink to="/" style={{ textDecoration: 'none', color: 'inherit' }}>
                        AthletiTrade
                    </RouterLink>
                </Typography>

                {}
                <RouterLink to="/login" style={{ color: 'white', marginRight: 20, textDecoration: 'none' }}>Login</RouterLink>
                <RouterLink to="/register" style={{ color: 'white', textDecoration: 'none' }}>Register</RouterLink>
            </Toolbar>
        </AppBar>
    );
};

export default Header;
