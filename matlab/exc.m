clear;close all;clc;
filename = 'files/all_150.xlsx';
verifyFile = 'files/wifi_has_speed.xlsx';
alpha = 0.01;
lambda = 0;
num_iters = 500;

data = readmatrix(filename);
% data = data(1:300, :);
X = data(:, 1:end-1);
y = data(:, end);
m = size(X, 1);

% feature normalize
[X, mu, sigma] = featureNormalize(X);

% add the bias unit which always equals to one
X = [ones(m, 1) X];


% compute the multivariate cost
theta = zeros(size(X, 2), 1);


[theta, J_history] = gradientDescentMulti(X, y, theta, alpha, num_iters, lambda);

% plot the convergence graph
figure(1);
% subplot(1, 2, 1);
plot(1:numel(J_history), J_history, '-b', 'LineWidth', 2);
xlabel('Number of iterations');
ylabel('Cost J');

% load verify data
v = readmatrix(verifyFile);

X_v = v(:, 1:end-1);
y_v = v(:, end);
[X_v, mu, sigma] = featureNormalize(X_v, mu, sigma);

% print the theta
sigma_t = [1 sigma];
f_theta = theta'

X_v = [ones(size(X_v, 1), 1) X_v];

H_v = X_v * theta;
result = H_v - y_v;
error = abs(result) ./ y_v;

mean_error = mean(error)

figure(2);
n_x = 1:size(H_v, 1);
% subplot(1, 2, 2);
plot(n_x, H_v, 'r-', n_x, y_v, 'g-');

% 将 error 输出成 excel
exportError2excel(error);
% 将 theta，mu，sigma 参数输出成excel
exportParams2Excel(theta, mu, sigma)

