import { Box, Button, Container, Grid, List, ListItem, ListItemText, Paper, Typography } from '@mui/material';
import React from 'react';
import Header from './Header';
import PortfolioSummary from './Portfolio.js';

const ownedPlayersData = [
  { name: 'Lebron James', tradingPrice: 150, shares: 10 },
  { name: 'Nikola Jokic', tradingPrice: 200, shares: 5 },
  { name: 'Stephen Curry', tradingPrice: 100, shares: 20 },
  { name: 'Kevin Durant', tradingPrice: 120, shares: 15 },
  { name: 'Giannis Antetokounmpo', tradingPrice: 180, shares: 8 },
  { name: 'Luka Doncic', tradingPrice: 220, shares: 12 },
  { name: 'Joel Embiid', tradingPrice: 160, shares: 7 },
  { name: 'Jayson Tatum', tradingPrice: 140, shares: 9 },
  { name: 'Ja Morant', tradingPrice: 130, shares: 11 },
  { name: 'Damian Lillard', tradingPrice: 110, shares: 14 },
  { name: 'Jimmy Butler', tradingPrice: 90, shares: 16 },
  { name: 'Kawhi Leonard', tradingPrice: 170, shares: 6 },
  { name: 'Anthony Davis', tradingPrice: 190, shares: 4 },
  { name: 'Zion Williamson', tradingPrice: 80, shares: 18 },
];

const fakeNews = [
  {
    headline: "LeBron James Hints at Retirement",
    content: "In a recent interview, LeBron James suggested he might be considering retirement after the next season. Fans are shocked!",
  },
  {
    headline: "Jokic Wins MVP Again!",
    content: "Nikola Jokic has been awarded the MVP title for the third year in a row. His dominance continues to impress.",
  },
  {
    headline: "Curry Breaks Three-Point Record",
    content: "Stephen Curry has once again broken his own record for most three-pointers in a season. Is he the greatest shooter of all time?",
  },
  {
    headline: "Zion Williamson to be traded?",
    content: "Rumors are swirling that Zion Williamson may be traded to a new team. Where will he end up?",
  },
  {
    headline: "New Rookie Sensation!",
    content: "A new rookie has burst onto the scene, showing incredible potential. Could they be the next big star?",
  },
  {
    headline: "Trade Deadline Approaching",
    content: "The trade deadline is just around the corner. Which teams will make big moves to improve their rosters?",
  },
  {
    headline: "Injury Update: Key Player Out",
    content: "A star player has suffered a significant injury and will be out for several weeks. How will this impact their team?",
  },
  {
    headline: "Coach Fired!",
    content: "A team has decided to part ways with their head coach. Who will take over?",
  },
];

const Layout = ({ children }) => {
  const buttonHeight = 70;
  const buttonGap = 16;
  const combinedButtonHeight = buttonHeight * 2 - buttonGap;

  return (
    <>
      <Header />
      <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
        <Grid container spacing={3}>
          <Grid item xs={12} md={9}>
            <PortfolioSummary />
            {}
            <Paper elevation={3} sx={{ mt: 3, height: combinedButtonHeight, p: 2, overflowY: 'auto', '&::-webkit-scrollbar': { display: 'none' }, }}>
              <Typography variant="h6" gutterBottom>
                News
              </Typography>
              <List>
                {fakeNews.map((newsItem, index) => (
                  <ListItem key={index} sx={{ p: 0, flexDirection: 'column', alignItems: 'flex-start' }}>
                    <ListItemText
                      primary={newsItem.headline}
                      secondary={newsItem.content}
                    />
                  </ListItem>
                ))}
              </List>
            </Paper>
          </Grid>
          <Grid item xs={12} md={3} sx={{ pl: 4 }}>
            <Box
              sx={{
                position: 'relative',
                p: 2,
                border: '1px dashed gray',
                overflowY: 'auto',
                maxHeight: 'calc(78vh - 200px)',
                marginBottom: 3,
                '&::-webkit-scrollbar': { display: 'none' },
                msOverflowStyle: 'none',
                scrollbarWidth: 'none',
              }}
            >
              <Box
                sx={{
                  position: 'sticky',
                  top: 0,
                  backgroundColor: 'white',
                  zIndex: 2,
                  width: '100%',
                  display: 'flex',
                  justifyContent: 'center',
                  alignItems: 'center',
                  paddingY: 1,
                  borderBottom: '1px solid lightgray',
                  marginBottom: 0,
                  marginTop: -2,
                }}
              >
                <Typography variant="h6">
                  Owned Players
                </Typography>
              </Box>
              <List sx={{ paddingTop: 0 }}>
                {ownedPlayersData.map((player, index) => (
                  <ListItem key={index} sx={{ p: 0 }}>
                    <ListItemText
                      primary={player.name}
                      secondary={`Price: $${player.tradingPrice} | Shares: ${player.shares}`}
                    />
                  </ListItem>
                ))}
              </List>
            </Box>
            {}
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 0 }}>
              <Button variant="contained" color="primary" fullWidth sx={{ height: buttonHeight }}>
                Trade
              </Button>
              <Button variant="outlined" color="primary" fullWidth sx={{ height: buttonHeight }}>
                Search Player
              </Button>
            </Box>
          </Grid>
        </Grid>
        <Box mt={4}>{children}</Box>
      </Container>
    </>
  );
};

export default Layout;
