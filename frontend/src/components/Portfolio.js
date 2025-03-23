import { Box, Paper, Typography } from '@mui/material';
import {
  CategoryScale,
  Chart as ChartJS,
  Legend,
  LinearScale,
  LineElement,
  PointElement,
  Title,
  Tooltip,
} from 'chart.js';
import { eachDayOfInterval, format, subMonths } from 'date-fns';
import React from 'react';
import { Line } from 'react-chartjs-2';

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend
);

const PortfolioSummary = () => {
  const generateMonthlyData = (numMonths) => {
    const data = [];
    const labels = [];

    const endDate = new Date();
    const startDate = subMonths(endDate, numMonths);
    const allDates = eachDayOfInterval({ start: startDate, end: endDate });

    allDates.forEach((date) => {
      const formattedDate = format(date, 'MMM d, yyyy');
      labels.push(formattedDate);

      const daysSinceStart = allDates.indexOf(date);
      const baseValue = 10000 + daysSinceStart * 5;
      const randomFluctuation = Math.random() * 200 - 100;
      const value = baseValue + randomFluctuation;
      data.push(value);
    });

    return { labels, data };
  };

  const { labels, data } = generateMonthlyData(3);

  const portfolioValue = data.length > 0 ? data[data.length - 1] : 0;

  const chartData = {
    labels: labels,
    datasets: [
      {
        label: 'Portfolio Value',
        data: data,
        fill: false,
        backgroundColor: 'rgb(75, 192, 192)',
        borderColor: 'rgba(75, 192, 192, 0.2)',
      },
    ],
  };

  const chartOptions = {
    responsive: true,
    plugins: {
      legend: {
        display: false,
      },
      title: {
        display: false,
      },
    },
    scales: {
      x: {
        ticks: {
          maxTicksLimit: 10,
          autoSkip: true,
        },
      },
    },
  };

  return (
    <Paper elevation={3} sx={{ p: 3 }}>
      <Box textAlign="center">
        <Typography variant="h5" gutterBottom>
          Your Portfolio
        </Typography>
        <Typography variant="h4" color="primary">
          ${portfolioValue.toLocaleString()}
        </Typography>
      </Box>
      <Box mt={3}>
        <Line data={chartData} options={chartOptions} />
      </Box>
    </Paper>
  );
};

export default PortfolioSummary;
